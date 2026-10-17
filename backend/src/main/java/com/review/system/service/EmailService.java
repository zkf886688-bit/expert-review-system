package com.review.system.service;

import com.review.system.mail.EmailSendResult;
import com.review.system.observability.AppMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppMetrics appMetrics;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:noreply@review.local}")
    private String from;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public EmailService(JavaMailSender mailSender, AppMetrics appMetrics) {
        this.mailSender = mailSender;
        this.appMetrics = appMetrics;
    }

    public EmailSendResult send(String to, String subject, String content) {
        if (to == null || to.isBlank()) {
            EmailSendResult result = EmailSendResult.builder()
                    .success(false)
                    .mode(mode())
                    .message("收件人为空")
                    .build();
            appMetrics.recordEmail(false);
            return result;
        }
        if (!enabled) {
            log.info("[邮件模拟] to={} subject={} content={}", to, subject, content);
            EmailSendResult result = EmailSendResult.builder()
                    .success(true)
                    .mode("mock")
                    .message("未启用 SMTP，内容已写入日志")
                    .build();
            appMetrics.recordEmail(true);
            return result;
        }
        if (mailHost == null || mailHost.isBlank()) {
            EmailSendResult result = EmailSendResult.builder()
                    .success(false)
                    .mode("smtp")
                    .message("已启用邮件但未配置 spring.mail.host")
                    .build();
            appMetrics.recordEmail(false);
            return result;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject != null ? subject : "通知");
            msg.setText(content != null ? content : "");
            mailSender.send(msg);
            appMetrics.recordEmail(true);
            return EmailSendResult.builder()
                    .success(true)
                    .mode("smtp")
                    .message("邮件已发送")
                    .build();
        } catch (Exception e) {
            log.warn("邮件发送失败: {}", e.getMessage());
            appMetrics.recordEmail(false);
            return EmailSendResult.builder()
                    .success(false)
                    .mode("smtp")
                    .message("发送失败: " + e.getMessage())
                    .build();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSmtpConfigured() {
        return mailHost != null && !mailHost.isBlank();
    }

    public String mode() {
        if (!enabled) {
            return "mock";
        }
        return isSmtpConfigured() ? "smtp" : "smtp-unconfigured";
    }
}
