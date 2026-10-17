package com.review.system.controller;

import com.review.system.common.Result;
import com.review.system.dto.*;
import com.review.system.mail.EmailSendResult;
import com.review.system.service.EmailService;
import com.review.system.service.NotificationTemplateService;
import com.review.system.service.SmsService;
import com.review.system.service.SysUserService;
import com.review.system.sms.SmsSendResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SysUserService sysUserService;
    private final NotificationTemplateService notificationTemplateService;
    private final HealthEndpoint healthEndpoint;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${spring.application.name:expert-review-system}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SystemInfoVO> info() {
        Status status = healthEndpoint.health().getStatus();
        return Result.ok(SystemInfoVO.builder()
                .appName(appName)
                .version(appVersion)
                .mailEnabled(emailService.isEnabled())
                .smsEnabled(smsService.isEnabled())
                .mailMode(emailService.mode())
                .smsProvider(smsService.activeProviderName())
                .mailConfigured(emailService.isSmtpConfigured())
                .smsConfigured(smsService.isProviderConfigured())
                .userCount(sysUserService.countUsers())
                .healthStatus(status.getCode())
                .build());
    }

    @PostMapping("/test-email")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<EmailSendResult> testEmail(@RequestBody SysUserController.TestEmailRequest req) {
        EmailSendResult result = emailService.send(req.getTo(), req.getSubject(), req.getContent());
        return Result.ok(result);
    }

    @PostMapping("/test-sms")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SmsSendResult> testSms(@Valid @RequestBody TestSmsRequest req) {
        String content = req.getContent() != null && !req.getContent().isBlank()
                ? req.getContent()
                : "外审专家管理系统短信测试";
        SmsSendResult result = smsService.sendWithResult(req.getPhone(), content);
        return Result.ok(result);
    }

    @GetMapping("/notification-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<NotificationTemplateVO> notificationTemplates() {
        return Result.ok(notificationTemplateService.listTemplates());
    }

    @PutMapping("/notification-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<NotificationTemplateVO> updateNotificationTemplates(
            @Valid @RequestBody NotificationTemplateUpdateRequest req) {
        notificationTemplateService.updateTemplates(req.getTemplates());
        return Result.ok(notificationTemplateService.listTemplates());
    }

    @PostMapping("/notification-templates/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<NotificationTemplateVO> resetNotificationTemplates() {
        notificationTemplateService.resetToDefaults();
        return Result.ok(notificationTemplateService.listTemplates());
    }

    @PostMapping("/notification-templates/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TemplatePreviewVO> previewNotificationTemplate(@Valid @RequestBody TemplatePreviewRequest req) {
        return Result.ok(notificationTemplateService.preview(req.getTemplateKey(), req.getVars()));
    }
}
