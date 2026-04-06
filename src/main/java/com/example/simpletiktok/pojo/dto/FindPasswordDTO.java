package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 找回密码参数。
 */
@Data
public class FindPasswordDTO {
    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * 邮箱验证码。
     */
    private String code;
    /**
     * 新密码。
     */
    private String newPassword;
}
