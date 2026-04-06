package com.example.simpletiktok.service;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

public interface LoginService {
    void captcha(String uuid, HttpServletResponse response) throws IOException;

    boolean sendEmailCode(String uuid, String code, String email);

    boolean checkEmailCode(String email, String code);

    boolean findPassword(String email, String code, String newPassword);
}
