package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 邮箱验证码提交参数。
 */
@Data
public class EmailCodeDTO {
    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * 邮箱验证码。
     */
    private String code;
}
