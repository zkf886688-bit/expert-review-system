package com.review.system.observability;

import com.review.system.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("notificationMail")
@RequiredArgsConstructor
public class MailHealthIndicator implements HealthIndicator {

    private final EmailService emailService;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Override
    public Health health() {
        if (!emailService.isEnabled()) {
            return Health.up()
                    .withDetail("mode", "mock")
                    .withDetail("configured", true)
                    .withDetail("message", "邮件未启用，通知写入日志")
                    .build();
        }
        if (!emailService.isSmtpConfigured()) {
            return Health.down()
                    .withDetail("mode", "smtp-unconfigured")
                    .withDetail("configured", false)
                    .withDetail("message", "已启用邮件但未配置 spring.mail.host")
                    .build();
        }
        return Health.up()
                .withDetail("mode", "smtp")
                .withDetail("configured", true)
                .withDetail("host", mailHost)
                .build();
    }
}
