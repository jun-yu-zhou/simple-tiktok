package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.simpletiktok.ai.TagExpandJudgeAiService;
import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;
import com.example.simpletiktok.pojo.vo.TagExpandResultVO;
import com.example.simpletiktok.service.ITagExpandJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagExpandJudgeServiceImpl implements ITagExpandJudgeService {

    private static final double BASE_LABEL_SCORE = 1D;
    private static final double EXPANDED_LABEL_SCORE = 0.4D;

    private final TagExpandJudgeAiService tagExpandJudgeAiService;

    @Override
    public TagExpandResultVO judge(List<String> videoLabels) {
        // 视频标签
        List<String> normalizedLabels = normalizeLabels(videoLabels);
        // 视频标签的提升权重为1
        Map<String, Double> labelScoreMap = initBaseLabelScoreMap(normalizedLabels);
        if (normalizedLabels.isEmpty()) {
            return new TagExpandResultVO(labelScoreMap, "点赞视频无有效标签，保持原状",
                    null, false, normalizedLabels);
        }
        try {
            // 调用模型，试图拿一个扩展标签
            TagExpandDecisionDTO decision = tagExpandJudgeAiService.judge(normalizedLabels);
            if (decision == null) {
                return new TagExpandResultVO(labelScoreMap, "模型返回为空，保持原始视频标签",
                        null, false, normalizedLabels);
            }

            String expandedLabel = StrUtil.trim(decision.getExpandedLabel());
            if (!Boolean.TRUE.equals(decision.getAccept())
                    || StrUtil.isBlank(expandedLabel)
                    || containsIgnoreCase(normalizedLabels, expandedLabel)) {
                return new TagExpandResultVO(
                        labelScoreMap,
                        StrUtil.blankToDefault(decision.getOpinion(), "未找到合适扩展标签，保持原始视频标签"),
                        null,
                        false,
                        normalizedLabels
                );
            }

            // 合并视频标签和扩展标签，扩展为0.4
            labelScoreMap.merge(expandedLabel, EXPANDED_LABEL_SCORE, Double::sum);
            return new TagExpandResultVO(
                    labelScoreMap,
                    StrUtil.blankToDefault(decision.getOpinion(), "找到一个合适扩展标签并写入兴趣模型"),
                    expandedLabel,
                    true,
                    normalizedLabels
            );
        } catch (Exception e) {
            log.warn("标签扩展判定失败，videoLabels={}", normalizedLabels, e);
            return new TagExpandResultVO(labelScoreMap, "模型判定异常，保持原始视频标签", null, false, normalizedLabels);
        }
    }

    private Map<String, Double> initBaseLabelScoreMap(List<String> labels) {
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        for (String label : labels) {
            scoreMap.merge(label, BASE_LABEL_SCORE, Double::sum);
        }
        return scoreMap;
    }

    private List<String> normalizeLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String label : labels) {
            String value = StrUtil.trim(label);
            if (StrUtil.isNotBlank(value)) {
                set.add(value);
            }
        }
        return new ArrayList<>(set);
    }

    // 判断扩展标签是否在视频便签内
    private boolean containsIgnoreCase(List<String> labels, String value) {
        if (labels == null || labels.isEmpty() || StrUtil.isBlank(value)) {
            return false;
        }
        String target = normalizeForCompare(value);
        for (String label : labels) {
            if (StrUtil.equalsIgnoreCase(normalizeForCompare(label), target)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForCompare(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return StrUtil.trim(value).replaceAll("\\s+", "");
    }
}
