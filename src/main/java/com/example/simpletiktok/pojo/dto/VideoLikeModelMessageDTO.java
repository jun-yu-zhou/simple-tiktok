package com.example.simpletiktok.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞行为消息体，用于异步更新兴趣模型。
 */
@Data
public class VideoLikeModelMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 视频 ID。
     */
    private Long videoId;
}
