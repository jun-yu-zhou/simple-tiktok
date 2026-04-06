package com.example.simpletiktok.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 视频分页结果。
 */
@Data
public class VideoPageVO {
    /**
     * 当前页。
     */
    private Long page;
    /**
     * 每页条数。
     */
    private Long limit;
    /**
     * 总记录数。
     */
    private Long total;
    /**
     * 当前页视频列表。
     */
    private List<VideoVO> records;
}
