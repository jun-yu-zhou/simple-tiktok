package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 关注/粉丝关系项。
 */
@Data
public class CustomerRelationVO {
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
     * 是否互相关注。
     */
    private Boolean each;
}
