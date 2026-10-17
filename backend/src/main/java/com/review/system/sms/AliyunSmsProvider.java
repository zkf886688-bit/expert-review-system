package com.review.system.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.system.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunSmsProvider implements SmsProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CONTENT_LEN = 450;

    private final SmsProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return "aliyun";
    }

    @Override
    public boolean isConfigured() {
        return properties.isAliyunConfigured();
    }

    @Override
    public SmsSendResult send(String phone, String content) {
        if (!isConfigured()) {
            return SmsSendResult.builder()
                    .success(false)
                    .provider(name())
                    .mode("aliyun")
                    .message("阿里云短信未配置完整（AccessKey/SignName/TemplateCode）")
                    .build();
        }
        SmsProperties.Aliyun cfg = properties.getAliyun();
        String normalizedPhone = SmsPhoneUtil.normalizeChinaMobile(phone);
        if (normalizedPhone == null) {
            return SmsSendResult.builder()
                    .success(false)
                    .provider(name())
                    .mode("aliyun")
                    .message("手机号格式无效")
                    .build();
        }
        String text = SmsPhoneUtil.truncate(content, MAX_CONTENT_LEN);
        String templateParam = buildTemplateParam(cfg.getTemplateParamKey(), text);
        TreeMap<String, String> business = new TreeMap<>();
        business.put("PhoneNumbers", normalizedPhone);
        business.put("SignName", cfg.getSignName());
        business.put("TemplateCode", cfg.getTemplateCode());
        business.put("TemplateParam", templateParam);

        Map<String, String> signed = AliyunRpcSigner.buildSignedParams(
                cfg.getAccessKeyId(),
                cfg.getAccessKeySecret(),
                "SendSms",
                "2017-05-25",
                business);
        String body = AliyunRpcSigner.toFormBody(signed);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getEndpoint()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = MAPPER.readTree(response.body());
            String code = json.path("Code").asText("");
            String message = json.path("Message").asText("");
            String requestId = json.path("RequestId").asText("");
            boolean ok = "OK".equalsIgnoreCase(code);
            if (!ok) {
                log.warn("阿里云短信失败 code={} message={} requestId={}", code, message, requestId);
            }
            return SmsSendResult.builder()
                    .success(ok)
                    .provider(name())
                    .mode("aliyun")
                    .message(ok ? "发送成功" : message)
                    .requestId(requestId)
                    .build();
        } catch (Exception e) {
            log.warn("阿里云短信请求异常: {}", e.getMessage());
            return SmsSendResult.builder()
                    .success(false)
                    .provider(name())
                    .mode("aliyun")
                    .message("请求失败: " + e.getMessage())
                    .build();
        }
    }

    private String buildTemplateParam(String key, String text) {
        try {
            return MAPPER.writeValueAsString(Map.of(key, text));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
