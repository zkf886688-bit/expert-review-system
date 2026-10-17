package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.system.common.BusinessException;
import com.review.system.config.SsoProperties;
import com.review.system.dto.LoginResponse;
import com.review.system.dto.SsoAuthorizeVO;
import com.review.system.dto.SsoProviderVO;
import com.review.system.entity.SysRole;
import com.review.system.entity.SysUser;
import com.review.system.mapper.SysRoleMapper;
import com.review.system.mapper.SysUserMapper;
import com.review.system.security.CustomUserDetailsService;
import com.review.system.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SsoService {

    private static final long STATE_TTL_SECONDS = 600;

    private final SsoProperties ssoProperties;
    private final AuthService authService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, Instant> oauthStates = new ConcurrentHashMap<>();

    public List<SsoProviderVO> listProviders() {
        if (!ssoProperties.isEnabled() && !ssoProperties.isMockEnabled() && !ssoProperties.getOauth2().isEnabled()) {
            return List.of();
        }
        List<SsoProviderVO> list = new ArrayList<>();
        if (ssoProperties.isMockEnabled()) {
            list.add(SsoProviderVO.builder()
                    .id("mock")
                    .name("演示 SSO（开发）")
                    .type("mock")
                    .loginMode("post")
                    .build());
        }
        if (ssoProperties.isEnabled() && ssoProperties.getOauth2().isEnabled()) {
            list.add(SsoProviderVO.builder()
                    .id("oauth2")
                    .name(ssoProperties.getOauth2().getDisplayName())
                    .type("oauth2")
                    .loginMode("redirect")
                    .build());
        }
        return list;
    }

    public LoginResponse mockLogin(String username) {
        assertMockEnabled();
        LoginUser user = (LoginUser) userDetailsService.loadUserByUsername(username.trim());
        return authService.issueTokenForUser(user, "SSO演示登录");
    }

    public SsoAuthorizeVO buildOAuth2AuthorizeUrl() {
        assertOAuth2Enabled();
        SsoProperties.OAuth2 cfg = ssoProperties.getOauth2();
        if (!StringUtils.hasText(cfg.getAuthorizationUri()) || !StringUtils.hasText(cfg.getClientId())) {
            throw new BusinessException("OAuth2 未配置 authorization-uri 或 client-id");
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        oauthStates.put(state, Instant.now().plusSeconds(STATE_TTL_SECONDS));
        String url = UriComponentsBuilder.fromUriString(cfg.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", cfg.getClientId())
                .queryParam("redirect_uri", cfg.getRedirectUri())
                .queryParam("scope", cfg.getScope())
                .queryParam("state", state)
                .build(true)
                .toUriString();
        return SsoAuthorizeVO.builder().authorizeUrl(url).state(state).build();
    }

    public String handleOAuth2Callback(String code, String state) {
        assertOAuth2Enabled();
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("缺少授权码");
        }
        Instant expiry = oauthStates.remove(state);
        if (expiry == null || Instant.now().isAfter(expiry)) {
            throw new BusinessException("SSO 状态无效或已过期，请重新登录");
        }
        JsonNode profile = exchangeCodeAndFetchProfile(code);
        SysUser user = resolveUser(profile);
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(user.getUsername());
        LoginResponse response = authService.issueTokenForUser(loginUser, "OAuth2 SSO登录");
        return buildFrontendRedirect(response.getToken());
    }

    private JsonNode exchangeCodeAndFetchProfile(String code) {
        SsoProperties.OAuth2 cfg = ssoProperties.getOauth2();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", cfg.getRedirectUri());
        form.add("client_id", cfg.getClientId());
        form.add("client_secret", cfg.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> tokenResp = restTemplate.exchange(
                cfg.getTokenUri(), HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
        if (!tokenResp.getStatusCode().is2xxSuccessful() || tokenResp.getBody() == null) {
            throw new BusinessException("OAuth2 换取令牌失败");
        }
        try {
            JsonNode tokenJson = objectMapper.readTree(tokenResp.getBody());
            String accessToken = tokenJson.path("access_token").asText(null);
            if (!StringUtils.hasText(accessToken)) {
                throw new BusinessException("OAuth2 响应缺少 access_token");
            }
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            ResponseEntity<String> userResp = restTemplate.exchange(
                    cfg.getUserInfoUri(), HttpMethod.GET, new HttpEntity<>(userHeaders), String.class);
            if (!userResp.getStatusCode().is2xxSuccessful() || userResp.getBody() == null) {
                throw new BusinessException("OAuth2 获取用户信息失败");
            }
            return objectMapper.readTree(userResp.getBody());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OAuth2 解析失败: {}", e.getMessage());
            throw new BusinessException("OAuth2 登录失败: " + e.getMessage());
        }
    }

    private SysUser resolveUser(JsonNode profile) {
        SsoProperties.OAuth2 cfg = ssoProperties.getOauth2();
        String username = textAt(profile, cfg.getUsernameAttribute());
        if (!StringUtils.hasText(username)) {
            username = textAt(profile, cfg.getEmailAttribute());
        }
        if (!StringUtils.hasText(username)) {
            username = textAt(profile, "sub");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("SSO 用户信息缺少可映射的用户名");
        }
        username = username.trim();

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            String email = textAt(profile, cfg.getEmailAttribute());
            if (StringUtils.hasText(email)) {
                user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, email));
            }
        }
        if (user != null) {
            return user;
        }
        if (!cfg.isAutoProvision()) {
            throw new BusinessException("SSO 用户未绑定本地账号: " + username);
        }
        return provisionUser(username, profile, cfg);
    }

    private SysUser provisionUser(String username, JsonNode profile, SsoProperties.OAuth2 cfg) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setRealName(textAt(profile, cfg.getNameAttribute()));
        user.setEmail(textAt(profile, cfg.getEmailAttribute()));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(1);
        sysUserMapper.insert(user);

        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, cfg.getDefaultRole()));
        if (role != null) {
            jdbcTemplate.update("""
                    INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)
                    """, user.getId(), role.getId());
        }
        return user;
    }

    private String textAt(JsonNode node, String field) {
        if (node == null || !StringUtils.hasText(field) || node.path(field).isMissingNode()) {
            return null;
        }
        String v = node.path(field).asText(null);
        return StringUtils.hasText(v) ? v : null;
    }

    private String buildFrontendRedirect(String token) {
        String template = ssoProperties.getFrontendCallbackUrl();
        if (!StringUtils.hasText(template)) {
            template = "http://localhost:5173/login/sso-callback?token={token}";
        }
        return template.replace("{token}", URLEncoder.encode(token, StandardCharsets.UTF_8));
    }

    private void assertMockEnabled() {
        if (!ssoProperties.isMockEnabled()) {
            throw new BusinessException("SSO 演示登录未启用");
        }
    }

    private void assertOAuth2Enabled() {
        if (!ssoProperties.isEnabled() || !ssoProperties.getOauth2().isEnabled()) {
            throw new BusinessException(503, "OAuth2 SSO 未启用");
        }
    }
}
