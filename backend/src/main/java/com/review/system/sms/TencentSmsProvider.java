package com.review.system.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.system.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TencentSmsProvider implements SmsProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERVICE = "sms";
    private static final String HOST = "sms.tencentcloudapi.com";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final int MAX_CONTENT_LEN = 450;

    private final SmsProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return "tencent";
    }

    @Override
    public boolean isConfigured() {
        return properties.isTencentConfigured();
    }

    @Override
    public SmsSendResult send(String phone, String content) {
        if (!isConfigured()) {
            return SmsSendResult.builder()
                    .success(false)
                    .provider(name())
                    .mode("tencent")
                    .message("腾讯云短信未配置完整（SecretId/SdkAppId/TemplateId 等）")
                    .build();
        }
        String e164 = SmsPhoneUtil.toE164China(phone);
        if (e164 == null) {
            return SmsSendResult.builder()
                    .success(false)
                    .provider(name())
                    .mode("tencent")
                    .message("手机号格式无效")
                    .build();
        }
        SmsProperties.Tencent cfg = properties.getTencent();
        String text = SmsPhoneUtil.truncate(content, MAX_CONTENT_LEN);
        try {
            String payload = MAPPER.writeValueAsString(Map.of(
                    "PhoneNumberSet", List.of(e164),
                    "SmsSdkAppId", cfg.getSdkAppId(),
                    "SignName", cfg.getSignName(),
                    "TemplateId", cfg.getTemplateId(),
                    "TemplateParamSet", List.of(text)
            ));
            long timestamp = Instant.now().getEpochSecond();
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(Instant.now());
            String authorization = buildAuthorization(cfg, payload, timestamp, date);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + HOST))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Host", HOST)
                    .header("X-TC-Action", "SendSms")
                    .header("X-TC-Version", "2021-01-11")
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .header("X-TC-Region", cfg.getRegion())
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = MAPPER.readTree(response.body());
            JsonNode sendStatus = json.path("Response").path("SendStatusSet").path(0);
            String code = sendStatus.path("Code").asText(json.path("Response").path("Error").path("Code").asText(""));
            String message = sendStatus.path("Message").asText(json.path("Response").path("Error").path("Message").asText(""));
            String requestId = json.path("Response").path("RequestId").asText("");
            boolean ok = "Ok".equalsIgnoreCase(code);
            if (!ok) {
                log.warn("腾讯云短信失败 code={} message={} requestId={}", code, message, requestId);
            }
            return SmsSendResult.builder()
                    .success(ok)
                    .provider(name())
                    .mode("tencent")
                    .message(ok ? "发送成功" : message)
                    .requestId(requestId)
                    .build();
        } catch (Exception e) {
            log.warn("腾讯云短信请求异常: {}", e.getMessage());
            return SmsSendResult.builder()
                    .success(false)
                    .provider(name())
                    .mode("tencent")
                    .message("请求失败: " + e.getMessage())
                    .build();
        }
    }

    private String buildAuthorization(SmsProperties.Tencent cfg, String payload, long timestamp, String date)
            throws Exception {
        String hashedPayload = sha256Hex(payload);
        String canonicalRequest = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + HOST + "\n\n"
                + "content-type;host\n" + hashedPayload;
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        byte[] secretDate = hmacSha256(("TC3" + cfg.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmacSha256(secretSigning, stringToSign));
        return ALGORITHM + " Credential=" + cfg.getSecretId() + "/" + credentialScope
                + ", SignedHeaders=content-type;host, Signature=" + signature;
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
