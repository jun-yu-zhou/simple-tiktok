package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 图片验证码校验参数。
 */
@Data
public class CaptchaDTO {
    /**
     * 验证码唯一标识。
     */
    private String uuid;
    /**
     * 用户输入的验证码。
     */
    private String code;
    /**
     * 业务邮箱（可选）。
     */
    private String email;
}
