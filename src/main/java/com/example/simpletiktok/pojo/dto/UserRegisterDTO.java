package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 用户注册参数。
 */
@Data
public class UserRegisterDTO {
    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * 登录密码。
     */
    private String password;
    /**
     * 昵称。
     */
    private String nickName;
    /**
     * 个性签名/简介。
     */
    private String description;
    /**
     * 性别。
     */
    private Integer sex;
    /**
     * 头像地址。
     */
    private String avatar;
}
