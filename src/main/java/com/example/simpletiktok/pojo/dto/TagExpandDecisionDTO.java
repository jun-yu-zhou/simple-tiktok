package com.example.simpletiktok.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 召回标签判定结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagExpandDecisionDTO {

    /**
     * 模型给出的简短意见。
     */
    private String opinion;

    /**
     * true: 允许写入召回标签；false: 跳过该召回标签。
     */
    private Boolean accept;
}
