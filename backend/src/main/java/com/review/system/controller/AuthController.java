package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.*;
import com.review.system.security.LoginUser;
import com.review.system.security.SecurityUtils;
import com.review.system.service.AuthService;
import com.review.system.service.CaptchaService;
import com.review.system.service.LoginLockoutService;
import com.review.system.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SsoService ssoService;
    private final CaptchaService captchaService;
    private final LoginLockoutService loginLockoutService;

    @Value("${app.captcha.enabled:true}")
    private boolean captchaEnabled;

    @GetMapping("/captcha")
    public Result<CaptchaService.CaptchaVO> captcha() {
        return Result.ok(captchaService.create());
    }

    @GetMapping(value = "/captcha/{id}/image", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> captchaImage(@PathVariable String id) {
        byte[] png = captchaService.image(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(png);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        loginLockoutService.assertNotLocked(request.getUsername(), clientIp);
        if (captchaEnabled) {
            captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
        }
        try {
            return Result.ok(authService.login(request, clientIp));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginLockoutService.recordFailure(request.getUsername(), clientIp);
            throw e;
        }
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/me")
    public Result<LoginResponse> me() {
        LoginUser user = SecurityUtils.currentUser();
        return Result.ok(authService.currentUser(user));
    }

    @PutMapping("/profile")
    public Result<LoginResponse> updateProfile(@RequestBody ProfileUpdateRequest req) {
        LoginUser user = SecurityUtils.currentUser();
        return Result.ok(authService.updateProfile(user, req));
    }

    @GetMapping("/sso/providers")
    public Result<List<SsoProviderVO>> ssoProviders() {
        return Result.ok(ssoService.listProviders());
    }

    @PostMapping("/sso/mock")
    public Result<LoginResponse> ssoMockLogin(@Valid @RequestBody SsoMockLoginRequest req) {
        return Result.ok(ssoService.mockLogin(req.getUsername()));
    }

    @GetMapping("/sso/oauth2/authorize")
    public Result<SsoAuthorizeVO> ssoOAuth2Authorize() {
        return Result.ok(ssoService.buildOAuth2AuthorizeUrl());
    }

    @GetMapping("/sso/oauth2/callback")
    public ResponseEntity<Void> ssoOAuth2Callback(@RequestParam String code,
                                                 @RequestParam(required = false) String state) {
        String redirect = ssoService.handleOAuth2Callback(code, state);
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, redirect).build();
    }
}
