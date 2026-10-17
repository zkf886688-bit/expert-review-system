package com.review.system.service;

import com.review.system.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginLockoutService {

    @Value("${app.auth.lockout.enabled:true}")
    private boolean enabled;

    @Value("${app.auth.lockout.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.lockout.lock-minutes:15}")
    private int lockMinutes;

    private final Map<String, AttemptRecord> records = new ConcurrentHashMap<>();

    public void assertNotLocked(String username, String clientIp) {
        if (!enabled || maxAttempts <= 0) {
            return;
        }
        AttemptRecord record = records.get(key(username, clientIp));
        if (record == null || record.lockedUntil == null) {
            return;
        }
        if (Instant.now().isBefore(record.lockedUntil)) {
            long minutes = Duration.between(Instant.now(), record.lockedUntil).toMinutes() + 1;
            throw new BusinessException(429, "登录失败次数过多，请约 " + minutes + " 分钟后再试");
        }
        records.remove(key(username, clientIp));
    }

    public void recordFailure(String username, String clientIp) {
        if (!enabled || maxAttempts <= 0) {
            return;
        }
        String key = key(username, clientIp);
        AttemptRecord record = records.computeIfAbsent(key, k -> new AttemptRecord());
        if (record.lockedUntil != null && Instant.now().isBefore(record.lockedUntil)) {
            return;
        }
        record.failures++;
        if (record.failures >= maxAttempts) {
            record.lockedUntil = Instant.now().plus(Duration.ofMinutes(Math.max(lockMinutes, 1)));
            record.failures = 0;
        }
    }

    public void clearFailures(String username, String clientIp) {
        if (username == null) {
            return;
        }
        records.remove(key(username, clientIp));
    }

    private static String key(String username, String clientIp) {
        String u = username != null ? username.trim().toLowerCase() : "";
        String ip = clientIp != null ? clientIp : "";
        return u + "|" + ip;
    }

    private static class AttemptRecord {
        private int failures;
        private Instant lockedUntil;
    }
}
