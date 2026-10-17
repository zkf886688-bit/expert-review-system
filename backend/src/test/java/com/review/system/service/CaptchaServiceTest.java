package com.review.system.service;

import com.review.system.common.BusinessException;
import com.review.system.service.captcha.InMemoryCaptchaStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaptchaServiceTest {

    private final CaptchaService service = new CaptchaService(new InMemoryCaptchaStore());

    @Test
    void createReturnsCaptchaId() {
        CaptchaService.CaptchaVO vo = service.create();
        assertNotNull(vo.captchaId());
        assertFalse(vo.captchaId().isBlank());
    }

    @Test
    void imageReturnsPngBytes() {
        CaptchaService.CaptchaVO vo = service.create();
        byte[] png = service.image(vo.captchaId());
        assertNotNull(png);
        assertTrue(png.length > 0);
        assertEquals((byte) 0x89, png[0]);
    }

    @Test
    void verifyRejectsBlankCode() {
        CaptchaService.CaptchaVO vo = service.create();
        assertThrows(BusinessException.class, () -> service.verify(vo.captchaId(), "  "));
    }

    @Test
    void verifyRejectsWrongCode() {
        CaptchaService.CaptchaVO vo = service.create();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify(vo.captchaId(), "WRNG"));
        assertEquals("验证码错误", ex.getMessage());
    }

    @Test
    void verifyRejectsExpiredIdAfterUse() {
        CaptchaService.CaptchaVO vo = service.create();
        assertThrows(BusinessException.class, () -> service.verify(vo.captchaId(), "WRNG"));
        assertThrows(BusinessException.class, () -> service.verify(vo.captchaId(), "WRNG"));
    }
}
