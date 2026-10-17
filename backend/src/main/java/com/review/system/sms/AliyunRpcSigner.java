package com.review.system.sms;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 阿里云 RPC OpenAPI 签名（SendSms 等接口通用）。
 */
public final class AliyunRpcSigner {

    private static final DateTimeFormatter ISO8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private AliyunRpcSigner() {
    }

    public static Map<String, String> buildSignedParams(
            String accessKeyId,
            String accessKeySecret,
            String action,
            String version,
            Map<String, String> businessParams) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", action);
        params.put("Format", "JSON");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", ISO8601.format(Instant.now()));
        params.put("Version", version);
        if (businessParams != null) {
            params.putAll(businessParams);
        }
        String signature = sign(params, accessKeySecret);
        params.put("Signature", signature);
        return params;
    }

    public static String sign(TreeMap<String, String> params, String accessKeySecret) {
        String canonicalized = params.entrySet().stream()
                .map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
                .collect(Collectors.joining("&"));
        String stringToSign = "POST&%2F&" + percentEncode(canonicalized);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] raw = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("阿里云签名失败", e);
        }
    }

    public static String toFormBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    static String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        return urlEncode(value)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
