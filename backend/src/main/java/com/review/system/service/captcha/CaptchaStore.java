package com.review.system.service.captcha;

public interface CaptchaStore {

    void put(String id, String answer, long expireAtMs);

    CaptchaEntry get(String id);

    CaptchaEntry remove(String id);

    void cleanupExpired();

    record CaptchaEntry(String answer, long expireAt) {
    }
}
