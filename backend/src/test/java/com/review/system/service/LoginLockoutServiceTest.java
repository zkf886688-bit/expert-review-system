package com.review.system.service;

import com.review.system.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class LoginLockoutServiceTest {

    private final LoginLockoutService service = new LoginLockoutService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "lockMinutes", 1);
    }

    @Test
    void locksAfterMaxFailures() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("user1", "127.0.0.1");
        }
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertNotLocked("user1", "127.0.0.1"));
        assertEquals(429, ex.getCode());
    }

    @Test
    void clearsOnSuccess() {
        service.recordFailure("user2", "10.0.0.1");
        service.clearFailures("user2", "10.0.0.1");
        assertDoesNotThrow(() -> service.assertNotLocked("user2", "10.0.0.1"));
    }
}
