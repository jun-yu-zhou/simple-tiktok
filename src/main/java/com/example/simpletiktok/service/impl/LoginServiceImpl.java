package com.example.simpletiktok.service.impl;

import com.example.simpletiktok.service.CaptchaService;
import com.example.simpletiktok.service.EmailService;
import com.example.simpletiktok.service.LoginService;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.util.RedisConstants;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final IUserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void captcha(String uuid, HttpServletResponse response) throws IOException {
        BufferedImage image = captchaService.getCaptcha(uuid);
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        out.flush();
    }

    @Override
    public boolean sendEmailCode(String uuid, String code, String email) {
        boolean ok = captchaService.validate(uuid, code);
        if (!ok) {
            return false;
        }
        String emailCode = randomSixCode();
        stringRedisTemplate.opsForValue().set(
                RedisConstants.EMAIL_CODE + email,
                emailCode,
                java.time.Duration.ofSeconds(RedisConstants.EMAIL_CODE_TTL)
        );
        emailService.send(email, "注册验证码", "注册验证码:" + emailCode + ",验证码5分钟之内有效");
        return true;
    }

    @Override
    public boolean checkEmailCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        String cached = stringRedisTemplate.opsForValue().get(RedisConstants.EMAIL_CODE + email);
        if (cached == null) {
            return false;
        }
        boolean ok = cached.equals(code);
        if (ok) {
            stringRedisTemplate.delete(RedisConstants.EMAIL_CODE + email);
        }
        return ok;
    }

    @Override
    public boolean findPassword(String email, String code, String newPassword) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(code) || StrUtil.isBlank(newPassword)) {
            return false;
        }
        if (!userService.existsByEmail(email)) {
            return false;
        }
        if (!checkEmailCode(email, code)) {
            return false;
        }
        return userService.updatePasswordByEmail(email, newPassword);
    }

    private String randomSixCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }
}
