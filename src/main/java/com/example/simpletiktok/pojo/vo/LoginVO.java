package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 登录成功返回对象。
 */
@Data
public class LoginVO {
    /**
     * 登录态令牌。
     */
    private String token;
    /**
     * 当前登录用户信息。
     */
    private UserVO user;
}
