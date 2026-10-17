package com.review.system.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockSmsProvider implements SmsProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public SmsSendResult send(String phone, String content) {
        log.info("[短信模拟] phone={} content={}", phone, content);
        return SmsSendResult.builder()
                .success(true)
                .provider(name())
                .mode("mock")
                .message("已写入日志（未启用真实短信）")
                .build();
    }
}
