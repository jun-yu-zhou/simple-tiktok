package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.simpletiktok.ai.TagExpandJudgeAiService;
import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;
import com.example.simpletiktok.service.ITagExpandJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagExpandJudgeServiceImpl implements ITagExpandJudgeService {

    private final TagExpandJudgeAiService tagExpandJudgeAiService;

    @Override
    public TagExpandDecisionDTO judge(Map<String, String> tagSourceMap) {
        if (tagSourceMap == null || tagSourceMap.isEmpty()) {
            return new TagExpandDecisionDTO("输入为空，跳过召回标签", false);
        }
        String candidateRecallTag = findCandidateRecallTag(tagSourceMap);
        if (StrUtil.isBlank(candidateRecallTag)) {
            return new TagExpandDecisionDTO("未找到召回标签，跳过", false);
        }
        try {
            String payload = JSONUtil.toJsonStr(tagSourceMap);
            TagExpandDecisionDTO result = tagExpandJudgeAiService.judge(payload);
            if (result == null || result.getAccept() == null) {
                return new TagExpandDecisionDTO("模型返回不完整，跳过召回标签", false);
            }
            result.setOpinion(StrUtil.blankToDefault(result.getOpinion(), "未提供意见"));
            result.setAccept(Boolean.TRUE.equals(result.getAccept()));
            return result;
        } catch (Exception e) {
            log.warn("标签扩展判定失败，candidateRecallTag={}", candidateRecallTag, e);
            return new TagExpandDecisionDTO("模型判定异常，跳过召回标签", false);
        }
    }

    private String findCandidateRecallTag(Map<String, String> tagSourceMap) {
        for (Map.Entry<String, String> entry : tagSourceMap.entrySet()) {
            if (!"source_recall".equals(entry.getValue())) {
                continue;
            }
            String tag = StrUtil.trim(entry.getKey());
            if (StrUtil.isNotBlank(tag)) {
                return tag;
            }
        }
        return null;
    }
}
