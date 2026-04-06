package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 用户资料更新参数。
 */
@Data
public class UserUpdateDTO {
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
