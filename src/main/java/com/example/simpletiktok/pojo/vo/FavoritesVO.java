package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 收藏夹信息返回对象。
 */
@Data
public class FavoritesVO {
    /**
     * 收藏夹 ID。
     */
    private Long id;
    /**
     * 收藏夹名称。
     */
    private String name;
    /**
     * 收藏夹描述。
     */
    private String description;
    /**
     * 收藏夹所属用户 ID。
     */
    private Long userId;
    /**
     * 收藏夹内视频数量。
     */
    private Long videoCount;
}
