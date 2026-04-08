package com.example.simpletiktok.service;

import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;

import java.util.Map;

public interface ITagExpandJudgeService {

    /**
     * 对单个召回标签做扩展判定。
     *
     * @param tagSourceMap 标签来源映射（key=标签，value=source_video/source_recall）
     * @return 判定结果（accept=true 才允许写入兴趣模型）
     */
    TagExpandDecisionDTO judge(Map<String, String> tagSourceMap);
}
