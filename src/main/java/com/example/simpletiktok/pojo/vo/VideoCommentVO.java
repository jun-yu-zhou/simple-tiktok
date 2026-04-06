package com.example.simpletiktok.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 评论展示对象。
 * 用于评论列表和按层级展开回复。
 */
@Data
public class VideoCommentVO {

    /**
     * 评论 ID。
     */
    private Long id;

    /**
     * 视频 ID。
     */
    private Long videoId;

    /**
     * 评论用户 ID。
     */
    private Long userId;

    /**
     * 评论用户昵称。
     */
    private String userNickName;

    /**
     * 评论用户头像。
     */
    private String userAvatar;

    /**
     * 被回复用户 ID（主评论为空）。
     */
    private Long replyToUserId;

    /**
     * 被回复用户昵称（主评论为空）。
     */
    private String replyToNickName;

    /**
     * 评论内容。
     */
    private String content;

    /**
     * 根评论 ID，主评论为 0。
     */
    private Long rootId;

    /**
     * 父评论 ID，主评论为 0。
     */
    private Long parentId;

    /**
     * 逻辑删除标记：0-未删，1-已删。
     */
    private Integer isDeleted;

    /**
     * 子评论数量。
     */
    private Long childCount;

    /**
     * 是否还有更多子评论可展开。
     */
    private Boolean hasMoreChildren;

    /**
     * 已展开的子评论列表。
     */
    private List<VideoCommentVO> children;

    /**
     * 创建时间（毫秒时间戳）。
     */
    private Long gmtCreated;

    /**
     * 更新时间（毫秒时间戳）。
     */
    private Long gmtUpdated;
}
