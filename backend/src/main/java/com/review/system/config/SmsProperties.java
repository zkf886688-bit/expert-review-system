package com.review.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private boolean enabled = false;
    /** mock | aliyun | tencent */
    private String provider = "mock";

    private Aliyun aliyun = new Aliyun();
    private Tencent tencent = new Tencent();

    @Data
    public static class Aliyun {
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String signName = "";
        private String templateCode = "";
        /** 短信模板变量名，如 content */
        private String templateParamKey = "content";
        private String endpoint = "https://dysmsapi.aliyuncs.com";
    }

    @Data
    public static class Tencent {
        private String secretId = "";
        private String secretKey = "";
        private String sdkAppId = "";
        private String signName = "";
        private String templateId = "";
        private String templateParamKey = "content";
        private String region = "ap-guangzhou";
    }

    public boolean isAliyunConfigured() {
        Aliyun a = aliyun;
        return notBlank(a.accessKeyId) && notBlank(a.accessKeySecret)
                && notBlank(a.signName) && notBlank(a.templateCode);
    }

    public boolean isTencentConfigured() {
        Tencent t = tencent;
        return notBlank(t.secretId) && notBlank(t.secretKey)
                && notBlank(t.sdkAppId) && notBlank(t.signName) && notBlank(t.templateId);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
