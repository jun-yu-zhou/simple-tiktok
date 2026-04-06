package com.example.simpletiktok.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 视频分类返回对象。
 */
@Data
public class TypeVO {
    /**
     * 分类 ID。
     */
    private Long id;
    /**
     * 分类名称。
     */
    private String name;
    /**
     * 分类描述。
     */
    private String description;
    /**
     * 可见性：0-私有，1-公开。
     */
    private Integer open;
    /**
     * 分类图标。
     */
    private String icon;
    /**
     * 排序值。
     */
    private Integer sort;
    /**
     * 分类绑定的标签名列表。
     */
    private List<String> labelNames;
}
