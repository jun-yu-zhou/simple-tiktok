package com.example.simpletiktok.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 视频发布/编辑参数。
 */
@Data
public class VideoSaveDTO {
    /**
     * 视频 ID，发布时可为空。
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
     * 标签名称列表。
     */
    private List<String> labels;
    /**
     * 可见性：0-私有，1-公开。
     */
    private Integer open;
    /**
     * 视频时长（文本格式）。
     */
    private String duration;
    /**
     * 分类 ID。
     */
    private Long typeId;
}
