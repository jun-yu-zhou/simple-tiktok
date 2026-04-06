package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 用户登录参数。
 */
@Data
public class UserLoginDTO {
    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * 登录密码。
     */
    private String password;
}
