package com.example.simpletiktok.service.impl;

import com.example.simpletiktok.service.CaptchaService;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:img:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    private final DefaultKaptcha producer;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public BufferedImage getCaptcha(String uuid) {
        String code = producer.createText();
        String key = CAPTCHA_KEY_PREFIX + uuid;
        stringRedisTemplate.opsForValue().set(key, code, CAPTCHA_TTL);
        return producer.createImage(code);
    }

    @Override
    public boolean validate(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }
        String key = CAPTCHA_KEY_PREFIX + uuid;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached == null) {
            return false;
        }
        stringRedisTemplate.delete(key);
        return cached.equals(code);
    }
}
