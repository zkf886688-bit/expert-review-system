package com.review.system.service;

import com.review.system.common.BusinessException;
import com.review.system.service.captcha.CaptchaStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    /** 去掉易混淆字符 0/O、1/I/l */
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final SecureRandom random = new SecureRandom();
    private final CaptchaStore captchaStore;

    public CaptchaVO create() {
        captchaStore.cleanupExpired();
        String code = randomCode();
        String id = UUID.randomUUID().toString().replace("-", "");
        captchaStore.put(id, code, System.currentTimeMillis() + TTL_MS);
        return new CaptchaVO(id);
    }

    public byte[] image(String id) {
        captchaStore.cleanupExpired();
        CaptchaStore.CaptchaEntry entry = captchaStore.get(id);
        if (entry == null || System.currentTimeMillis() > entry.expireAt()) {
            throw new BusinessException(404, "验证码已过期");
        }
        return renderPng(entry.answer());
    }

    public void verify(String id, String code) {
        if (id == null || code == null || code.isBlank()) {
            throw new BusinessException(400, "请完成验证码");
        }
        captchaStore.cleanupExpired();
        CaptchaStore.CaptchaEntry entry = captchaStore.remove(id);
        if (entry == null || System.currentTimeMillis() > entry.expireAt()) {
            throw new BusinessException(400, "验证码已过期");
        }
        if (!entry.answer().equalsIgnoreCase(code.trim())) {
            throw new BusinessException(400, "验证码错误");
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private byte[] renderPng(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(248, 250, 252));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(160, 210));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            int charWidth = WIDTH / (CODE_LENGTH + 1);
            for (int i = 0; i < code.length(); i++) {
                g.setFont(new Font("Arial", Font.BOLD, 28 + random.nextInt(4)));
                g.setColor(randomColor(30, 120));
                double angle = (random.nextDouble() - 0.5) * 0.35;
                int x = charWidth * (i + 1) - 8;
                int y = 30 + random.nextInt(6);
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }

            for (int i = 0; i < 30; i++) {
                g.setColor(randomColor(140, 200));
                g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }

            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(226, 232, 240));
            g.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 8, 8);
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("验证码生成失败");
        }
    }

    private Color randomColor(int min, int max) {
        return new Color(
                min + random.nextInt(max - min),
                min + random.nextInt(max - min),
                min + random.nextInt(max - min));
    }

    public record CaptchaVO(String captchaId) {
    }
}
