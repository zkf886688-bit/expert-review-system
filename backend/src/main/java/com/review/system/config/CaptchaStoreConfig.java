package com.review.system.config;

import com.review.system.service.captcha.CaptchaStore;
import com.review.system.service.captcha.InMemoryCaptchaStore;
import com.review.system.service.captcha.RedisCaptchaStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CaptchaStoreConfig {

    @Bean
    @ConditionalOnProperty(name = "app.captcha.redis-enabled", havingValue = "true")
    public CaptchaStore redisCaptchaStore(StringRedisTemplate redisTemplate) {
        return new RedisCaptchaStore(redisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.captcha.redis-enabled", havingValue = "false", matchIfMissing = true)
    public CaptchaStore inMemoryCaptchaStore() {
        return new InMemoryCaptchaStore();
    }
}
