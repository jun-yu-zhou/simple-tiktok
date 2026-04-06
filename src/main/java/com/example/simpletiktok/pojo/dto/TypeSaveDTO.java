package com.example.simpletiktok.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 分类保存参数。
 */
@Data
public class TypeSaveDTO {

    /**
     * 分类 ID。为空表示新增，不为空表示编辑。
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
     * 标签名称列表。
     */
    private List<String> labelNames;
}
