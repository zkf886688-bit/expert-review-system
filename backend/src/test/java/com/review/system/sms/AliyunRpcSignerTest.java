package com.review.system.sms;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class AliyunRpcSignerTest {

    @Test
    void signIsDeterministicForFixedInput() {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", "testKey");
        params.put("Action", "SendSms");
        params.put("Format", "JSON");
        params.put("PhoneNumbers", "13800138000");
        params.put("SignName", "测试签名");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", "fixed-nonce");
        params.put("SignatureVersion", "1.0");
        params.put("TemplateCode", "SMS_123");
        params.put("TemplateParam", "{\"content\":\"hi\"}");
        params.put("Timestamp", "2026-05-31T00:00:00Z");
        params.put("Version", "2017-05-25");

        String sig1 = AliyunRpcSigner.sign(params, "testSecret");
        String sig2 = AliyunRpcSigner.sign(params, "testSecret");

        assertEquals(sig1, sig2);
        assertFalse(sig1.isBlank());
    }

    @Test
    void normalizeChinaMobile() {
        assertEquals("13800138000", SmsPhoneUtil.normalizeChinaMobile("+86 138-0013-8000"));
        assertNull(SmsPhoneUtil.normalizeChinaMobile("12345"));
    }
}
