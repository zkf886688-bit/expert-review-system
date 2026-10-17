package com.review.system.service;

import com.review.system.config.SmsProperties;
import com.review.system.observability.AppMetrics;
import com.review.system.sms.MockSmsProvider;
import com.review.system.sms.SmsSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private MockSmsProvider mockProvider;

    @Mock
    private com.review.system.sms.AliyunSmsProvider aliyunProvider;

    @Mock
    private com.review.system.sms.TencentSmsProvider tencentProvider;

    @Mock
    private AppMetrics appMetrics;

    private SmsProperties properties;

    private SmsService smsService;

    @BeforeEach
    void setUp() {
        properties = new SmsProperties();
        lenient().when(mockProvider.name()).thenReturn("mock");
        lenient().when(aliyunProvider.name()).thenReturn("aliyun");
        lenient().when(tencentProvider.name()).thenReturn("tencent");
        smsService = new SmsService(properties, mockProvider, aliyunProvider, tencentProvider, appMetrics);
    }

    @Test
    void usesMockWhenDisabled() {
        properties.setEnabled(false);
        when(mockProvider.send("13800138000", "hello")).thenReturn(
                SmsSendResult.builder().success(true).provider("mock").mode("mock").message("ok").build());

        SmsSendResult result = smsService.sendWithResult("13800138000", "hello");

        assertTrue(result.isSuccess());
        assertEquals("mock", smsService.activeProviderName());
    }

    @Test
    void blankPhoneReturnsFailure() {
        SmsSendResult result = smsService.sendWithResult("", "hello");
        assertFalse(result.isSuccess());
        assertEquals("手机号为空", result.getMessage());
    }
}
