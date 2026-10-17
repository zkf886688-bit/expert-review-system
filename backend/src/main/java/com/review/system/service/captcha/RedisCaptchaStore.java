package com.review.system.service.captcha;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@RequiredArgsConstructor
public class RedisCaptchaStore implements CaptchaStore {

    private static final String PREFIX = "captcha:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void put(String id, String answer, long expireAtMs) {
        long ttlMs = Math.max(expireAtMs - System.currentTimeMillis(), 1000);
        redisTemplate.opsForValue().set(key(id), answer + "|" + expireAtMs, Duration.ofMillis(ttlMs));
    }

    @Override
    public CaptchaEntry get(String id) {
        String raw = redisTemplate.opsForValue().get(key(id));
        return parse(raw);
    }

    @Override
    public CaptchaEntry remove(String id) {
        String raw = redisTemplate.opsForValue().getAndDelete(key(id));
        return parse(raw);
    }

    @Override
    public void cleanupExpired() {
        // Redis TTL handles expiry
    }

    private static CaptchaEntry parse(String raw) {
        if (raw == null || !raw.contains("|")) {
            return null;
        }
        int sep = raw.lastIndexOf('|');
        return new CaptchaEntry(raw.substring(0, sep), Long.parseLong(raw.substring(sep + 1)));
    }

    private static String key(String id) {
        return PREFIX + id;
    }
}
