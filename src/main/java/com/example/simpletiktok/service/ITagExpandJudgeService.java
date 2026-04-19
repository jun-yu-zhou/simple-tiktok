package com.example.simpletiktok.service;

import com.example.simpletiktok.pojo.vo.TagExpandResultVO;

import java.util.List;

public interface ITagExpandJudgeService {

    /**
     * 执行一轮标签扩展判定，返回可直接用于兴趣模型更新的结果。
     *
     * @param videoLabels 点赞视频标签列表（调用方已做基础清洗）
     * @return 包含标签权重映射与判定说明的结果对象
     */
    TagExpandResultVO judge(List<String> videoLabels);
}
