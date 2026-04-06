package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 收藏弹窗中的收藏夹状态返回对象。
 * 用于告知前端当前视频在每个收藏夹中的选中状态。
 */
@Data
public class FavoriteSelectStateVO {
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

    /**
     * 当前视频是否已在该收藏夹中。
     */
    private Boolean hasVideo;
}
