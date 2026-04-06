package com.example.simpletiktok.controller;

import com.example.simpletiktok.pojo.dto.CaptchaDTO;
import com.example.simpletiktok.pojo.dto.EmailCodeDTO;
import com.example.simpletiktok.pojo.dto.FindPasswordDTO;
import com.example.simpletiktok.service.LoginService;
import com.example.simpletiktok.util.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    /**
     * 获取图形验证码（uuid 保证验证码唯一）。
     *
     * 测试地址:
     * GET http://localhost:8080/api/login/captcha.jpg/{uuid}
     *
     * 测试示例:
     * curl "http://localhost:8080/api/login/captcha.jpg/123e4567-e89b-12d3-a456-426614174000"
     */
    @GetMapping("/captcha.jpg/{uuid}")
    public void captcha(HttpServletResponse response, @PathVariable String uuid) throws IOException {
        loginService.captcha(uuid, response);
    }

    /**
     * 获取邮箱验证码（先校验图形验证码）。
     *
     * 测试地址:
     * POST http://localhost:8080/api/login/getCode
     *
     * 测试示例:
     * curl -X POST "http://localhost:8080/api/login/getCode" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"uuid\":\"123e4567-e89b-12d3-a456-426614174000\",\"code\":\"abcd\",\"email\":\"test@example.com\"}"
     */
    @PostMapping("/getCode")
    public R<?> getCode(@RequestBody CaptchaDTO req) {
        boolean ok = loginService.sendEmailCode(req.getUuid(), req.getCode(), req.getEmail());
        return ok ? R.ok().message("发送成功，请耐心等待") : R.error().message("验证码错误");
    }

    /**
     * 校验邮箱验证码。
     *
     * 测试地址:
     * POST http://localhost:8080/api/login/check
     *
     * 测试示例:
     * curl -X POST "http://localhost:8080/api/login/check" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"email\":\"test@example.com\",\"code\":\"123456\"}"
     */
    @PostMapping("/check")
    public R<?> check(@RequestBody EmailCodeDTO req) {
        boolean ok = loginService.checkEmailCode(req.getEmail(), req.getCode());
        return ok ? R.ok().message("验证成功") : R.error().message("验证码不正确");
    }

    /**
     * 找回密码（通过邮箱验证码重置密码）。
     *
     * 测试地址:
     * POST http://localhost:8080/api/login/findPassword
     *
     * 测试示例:
     * curl -X POST "http://localhost:8080/api/login/findPassword" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"email\":\"test@example.com\",\"code\":\"123456\",\"newPassword\":\"new123456\"}"
     */
    @PostMapping("/findPassword")
    public R<?> findPassword(@RequestBody FindPasswordDTO req) {
        boolean ok = loginService.findPassword(req.getEmail(), req.getCode(), req.getNewPassword());
        return ok ? R.ok().message("修改成功") : R.error().message("修改失败，验证码不正确或邮箱不存在");
    }
}
