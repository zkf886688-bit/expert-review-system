package com.review.system.service;

import com.review.system.config.SmsProperties;
import com.review.system.observability.AppMetrics;
import com.review.system.sms.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmsService {

    private final SmsProperties properties;
    private final MockSmsProvider mockProvider;
    private final Map<String, SmsProvider> providersByName;
    private final AppMetrics appMetrics;

    public SmsService(
            SmsProperties properties,
            MockSmsProvider mockProvider,
            AliyunSmsProvider aliyunSmsProvider,
            TencentSmsProvider tencentSmsProvider,
            AppMetrics appMetrics) {
        this.properties = properties;
        this.mockProvider = mockProvider;
        this.appMetrics = appMetrics;
        this.providersByName = List.of(mockProvider, aliyunSmsProvider, tencentSmsProvider).stream()
                .collect(Collectors.toMap(SmsProvider::name, Function.identity()));
    }

    public void send(String phone, String content) {
        sendWithResult(phone, content);
    }

    public SmsSendResult sendWithResult(String phone, String content) {
        if (phone == null || phone.isBlank()) {
            appMetrics.recordSms(false);
            return SmsSendResult.builder()
                    .success(false)
                    .provider(activeProviderName())
                    .mode(mode())
                    .message("手机号为空")
                    .build();
        }
        SmsProvider provider = resolveProvider();
        SmsSendResult result = provider.send(phone, content);
        appMetrics.recordSms(result.isSuccess());
        if (!result.isSuccess()) {
            log.warn("短信发送失败 provider={} phone={} msg={}", result.getProvider(), phone, result.getMessage());
        }
        return result;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String configuredProviderName() {
        return properties.getProvider() != null ? properties.getProvider().toLowerCase(Locale.ROOT) : "mock";
    }

    public String activeProviderName() {
        if (!properties.isEnabled()) {
            return "mock";
        }
        return configuredProviderName();
    }

    public boolean isProviderConfigured() {
        if (!properties.isEnabled()) {
            return true;
        }
        return resolveProvider().isConfigured();
    }

    public String mode() {
        if (!properties.isEnabled()) {
            return "mock";
        }
        return isProviderConfigured() ? configuredProviderName() : configuredProviderName() + "-unconfigured";
    }

    private SmsProvider resolveProvider() {
        if (!properties.isEnabled()) {
            return mockProvider;
        }
        String name = configuredProviderName();
        SmsProvider provider = providersByName.get(name);
        if (provider == null) {
            log.warn("未知短信 provider={}，回退 mock", name);
            return mockProvider;
        }
        return provider;
    }
}
