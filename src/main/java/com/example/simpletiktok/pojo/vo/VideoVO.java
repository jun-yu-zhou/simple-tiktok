package com.example.simpletiktok.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 视频详情返回对象。
 */
@Data
public class VideoVO {
    /**
     * 视频 ID。
     */
    private Long id;
    /**
     * 视频文案。
     */
    private String caption;
    /**
     * 视频文件名。
     */
    private String videoFileName;
    /**
     * 封面文件名。
     */
    private String coverFileName;
    /**
     * 视频播放地址。
     */
    private String videoUrl;
    /**
     * 封面访问地址。
     */
    private String coverUrl;
    /**
     * 标签名称列表。
     */
    private List<String> labels;
    /**
     * 作者用户 ID。
     */
    private Long userId;
    /**
     * 作者昵称。
     */
    private String userNickName;
    /**
     * 作者头像。
     */
    private String userAvatar;
    /**
     * 当前登录用户是否已关注作者。
     */
    private Boolean followedAuthor;
    /**
     * 可见性：0-私有，1-公开。
     */
    private Integer open;
    /**
     * 审核状态。
     */
    private Integer auditStatus;
    /**
     * 审核消息。
     */
    private String msg;
    /**
     * 播放量。
     */
    private Long viewCount;
    /**
     * 点赞量。
     */
    private Long likeCount;
    /**
     * 分享量。
     */
    private Long shareCount;
    /**
     * 收藏量。
     */
    private Long favoriteCount;
    /**
     * 视频时长（文本格式）。
     */
    private String duration;
    /**
     * 分类 ID。
     */
    private Long typeId;
    /**
     * 创建时间（毫秒时间戳）。
     */
    private Long gmtCreated;
    /**
     * 更新时间（毫秒时间戳）。
     */
    private Long gmtUpdated;
}
