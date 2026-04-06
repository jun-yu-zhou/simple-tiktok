package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 评论发布参数。
 * 支持主评论和回复评论：
 * 主评论：rootId=0，parentId=0；
 * 回复评论：rootId=主评论ID，parentId=直接父评论ID。
 */
@Data
public class VideoCommentSaveDTO {

    /**
     * 视频 ID。
     */
    private Long videoId;

    /**
     * 评论内容。
     */
    private String content;

    /**
     * 根评论 ID，主评论传 0。
     */
    private Long rootId;

    /**
     * 父评论 ID，主评论传 0。
     */
    private Long parentId;
}
