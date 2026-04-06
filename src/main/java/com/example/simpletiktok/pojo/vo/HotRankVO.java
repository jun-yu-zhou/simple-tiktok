package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 热榜视频项返回对象。
 */
@Data
public class HotRankVO {
    /**
     * 视频 ID。
     */
    private Long videoId;
    /**
     * 视频文案。
     */
    private String caption;
    /**
     * 热度分值。
     */
    private Double hot;
    /**
     * 格式化后的热度展示文本。
     */
    private String hotFormat;
}
