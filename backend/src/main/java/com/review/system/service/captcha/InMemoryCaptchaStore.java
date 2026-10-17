package com.review.system.service.captcha;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCaptchaStore implements CaptchaStore {

    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String id, String answer, long expireAtMs) {
        store.put(id, new CaptchaEntry(answer, expireAtMs));
    }

    @Override
    public CaptchaEntry get(String id) {
        return store.get(id);
    }

    @Override
    public CaptchaEntry remove(String id) {
        return store.remove(id);
    }

    @Override
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt() < now);
    }
}
