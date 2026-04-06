package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 管理员登录参数。
 */
@Data
public class AdminLoginDTO {

    /**
     * 管理员账号。
     */
    private String username;

    /**
     * 管理员密码。
     */
    private String password;
}
