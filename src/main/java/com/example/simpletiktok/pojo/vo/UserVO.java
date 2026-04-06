package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 用户信息返回对象。
 */
@Data
public class UserVO {
    /**
     * 用户 ID。
     */
    private Long id;
    /**
     * 邮箱地址。
     */
    private String email;
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

    /**
     * 默认收藏夹 ID。
     */
    private Long defaultFavoritesId;
}
