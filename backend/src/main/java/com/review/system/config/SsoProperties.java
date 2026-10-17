package com.review.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sso")
public class SsoProperties {

    /** 是否启用 SSO 相关 API */
    private boolean enabled = false;

    /** 开发/demo：允许 POST /api/auth/sso/mock 按用户名直接登录 */
    private boolean mockEnabled = true;

    /** OAuth 成功后重定向到前端的地址（可带 {token} 占位符） */
    private String frontendCallbackUrl = "http://localhost:5173/login/sso-callback?token={token}";

    private OAuth2 oauth2 = new OAuth2();

    @Data
    public static class OAuth2 {
        private boolean enabled = false;
        private String displayName = "企业统一认证";
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        /** 在 OAuth 提供方注册的回调地址，应指向后端 /api/auth/sso/oauth2/callback */
        private String redirectUri = "http://localhost:8080/api/auth/sso/oauth2/callback";
        private String scope = "openid profile email";
        private String usernameAttribute = "preferred_username";
        private String emailAttribute = "email";
        private String nameAttribute = "name";
        /** 未匹配到本地用户时是否自动创建账号 */
        private boolean autoProvision = false;
        /** 自动创建账号时的默认角色 */
        private String defaultRole = "EXPERT";
    }
}
