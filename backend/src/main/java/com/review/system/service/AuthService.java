package com.review.system.service;

import com.review.system.observability.AppMetrics;
import com.review.system.common.BusinessException;
import com.review.system.dto.LoginRequest;
import com.review.system.dto.LoginResponse;
import com.review.system.dto.ProfileUpdateRequest;
import com.review.system.entity.SysUser;
import com.review.system.mapper.SysUserMapper;
import com.review.system.security.JwtUtil;
import com.review.system.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final AuditService auditService;
    private final AppMetrics appMetrics;
    private final LoginLockoutService loginLockoutService;

    public LoginResponse login(LoginRequest request, String clientIp) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser user = (LoginUser) auth.getPrincipal();
        loginLockoutService.clearFailures(request.getUsername(), clientIp);
        return issueTokenForUser(user, "用户登录");
    }

    public LoginResponse issueTokenForUser(LoginUser user, String auditDetail) {
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRoles());
        SysUser entity = sysUserMapper.selectById(user.getUserId());
        auditService.log("LOGIN", "USER", user.getUserId(), auditDetail);
        appMetrics.recordLoginSuccess();
        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(entity != null ? entity.getRealName() : null)
                .email(entity != null ? entity.getEmail() : null)
                .phone(entity != null ? entity.getPhone() : null)
                .expertId(user.getExpertId())
                .roles(user.getRoles())
                .build();
    }

    public LoginResponse currentUser(LoginUser user) {
        SysUser entity = sysUserMapper.selectById(user.getUserId());
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        return toLoginResponse(user, entity);
    }

    public LoginResponse updateProfile(LoginUser user, ProfileUpdateRequest req) {
        SysUser entity = sysUserMapper.selectById(user.getUserId());
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        if (req.getRealName() != null) entity.setRealName(req.getRealName());
        if (req.getEmail() != null) entity.setEmail(req.getEmail());
        if (req.getPhone() != null) entity.setPhone(req.getPhone());
        sysUserMapper.updateById(entity);
        auditService.log("UPDATE_PROFILE", "USER", entity.getId(), "更新个人资料");
        return toLoginResponse(user, entity);
    }

    private LoginResponse toLoginResponse(LoginUser user, SysUser entity) {
        return LoginResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(entity.getRealName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .expertId(user.getExpertId())
                .roles(user.getRoles())
                .build();
    }
}
