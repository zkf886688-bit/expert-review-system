package com.review.system.observability;

import com.review.system.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("notificationSms")
@RequiredArgsConstructor
public class SmsHealthIndicator implements HealthIndicator {

    private final SmsService smsService;

    @Override
    public Health health() {
        if (!smsService.isEnabled()) {
            return Health.up()
                    .withDetail("mode", "mock")
                    .withDetail("provider", "mock")
                    .withDetail("configured", true)
                    .withDetail("message", "短信未启用，通知写入日志")
                    .build();
        }
        String provider = smsService.activeProviderName();
        boolean configured = smsService.isProviderConfigured();
        if (!configured) {
            return Health.down()
                    .withDetail("mode", smsService.mode())
                    .withDetail("provider", provider)
                    .withDetail("configured", false)
                    .withDetail("message", "短信 provider 未完整配置")
                    .build();
        }
        return Health.up()
                .withDetail("mode", smsService.mode())
                .withDetail("provider", provider)
                .withDetail("configured", true)
                .build();
    }
}
