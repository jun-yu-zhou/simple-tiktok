package com.example.simpletiktok.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频评论实体，对应表 video_comment。
 */
@Data
@TableName("video_comment")
public class VideoComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 视频ID
     */
    @TableField("video_id")
    private Long videoId;

    /**
     * 评论用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 根评论ID，主评论为0
     */
    @TableField("root_id")
    private Long rootId;

    /**
     * 父评论ID，主评论为0
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 逻辑删除：0未删，1已删
     */
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    /**
     * 更新时间
     */
    @TableField("gmt_updated")
    private LocalDateTime gmtUpdated;
}

