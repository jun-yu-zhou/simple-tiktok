package com.example.simpletiktok.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 标签扩展最终结果，用于更新兴趣模型与日志记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagExpandResultVO {

    /**
     * 最终标签与权重映射：包含视频原始标签，以及最多一个扩展标签。
     */
    private Map<String, Double> labelScoreMap;

    /**
     * 判定说明：为什么接受扩展标签，或为什么未找到合适标签。
     */
    private String opinion;

    /**
     * 模型选中的扩展标签；为空表示没有扩展。
     */
    private String expandedLabel;

    /**
     * 扩展标签是否被接受并合并进最终结果。
     */
    private Boolean expandedAccepted;

    /**
     * 参与判定的标准化视频标签列表。
     */
    private List<String> videoLabels;
}
