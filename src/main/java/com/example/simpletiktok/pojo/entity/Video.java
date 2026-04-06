package com.example.simpletiktok.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "video", autoResultMap = true)
public class Video implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String caption;

    @TableField("video_file_name")
    private String videoFileName;

    @TableField("cover_file_name")
    private String coverFileName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> labels;

    @TableField("user_id")
    private Long userId;

    private Integer open;

    @TableField("audit_status")
    private Integer auditStatus;

    private String msg;

    @TableField("view_count")
    private Long viewCount;

    @TableField("like_count")
    private Long likeCount;

    @TableField("share_count")
    private Long shareCount;

    @TableField("favorite_count")
    private Long favoriteCount;

    private String duration;

    @TableField("type_id")
    private Long typeId;

    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @TableField("gmt_updated")
    private LocalDateTime gmtUpdated;
}
