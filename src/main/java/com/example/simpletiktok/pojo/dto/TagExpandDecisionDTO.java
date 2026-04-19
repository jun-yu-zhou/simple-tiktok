package com.example.simpletiktok.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签扩展的模型判定结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagExpandDecisionDTO {

    /**
     * 模型给出的判定说明。
     */
    private String opinion;

    /**
     * 是否允许将扩展标签写入用户兴趣模型。
     */
    private Boolean accept;

    /**
     * 模型选出的候选扩展标签；为空表示没有可用候选。
     */
    private String expandedLabel;
}
