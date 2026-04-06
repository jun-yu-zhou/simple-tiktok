package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 用户主页信息返回对象。
 */
@Data
public class CustomerInfoVO {
    /**
     * 用户 ID。
     */
    private Long id;
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
     * 关注数。
     */
    private Long followCount;
    /**
     * 粉丝数。
     */
    private Long fansCount;
}
