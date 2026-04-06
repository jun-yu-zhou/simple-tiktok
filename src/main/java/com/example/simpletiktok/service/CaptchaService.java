package com.example.simpletiktok.service;

import java.awt.image.BufferedImage;

public interface CaptchaService {
    BufferedImage getCaptcha(String uuid);

    boolean validate(String uuid, String code);
}
