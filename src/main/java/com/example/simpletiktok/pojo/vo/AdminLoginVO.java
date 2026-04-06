package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 管理员登录结果。
 */
@Data
public class AdminLoginVO {

    /**
     * 登录令牌（JWT，无状态）。
     */
    private String token;

    /**
     * 管理员 ID。
     */
    private Long id;

    /**
     * 管理员账号。
     */
    private String username;
}
