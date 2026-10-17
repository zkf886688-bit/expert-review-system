package com.review.system.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 验证码未使用 Redis 时，不加载 Redis 客户端，避免本地无 Redis 时健康检查误报 DOWN。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.captcha.redis-enabled", havingValue = "false", matchIfMissing = true)
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
public class RedisOptionalConfig {
}
