package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;
import com.example.simpletiktok.service.ITagExpandJudgeService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagExpandJudgeServiceImpl implements ITagExpandJudgeService {

    private static final String SYSTEM_PROMPT = """
            你是推荐系统标签扩展判定助手。
            输入是 tagSourceMap，key 是标签，value 是 source_video 或 source_recall。
            请先识别 value=source_recall 的标签作为待判定扩展标签，再结合所有 source_video 标签判断是否应写入兴趣模型。
            若语义相关且不偏题则 accept=true，若语义弱相关、过泛或噪声则 accept=false。
            输出必须严格为 JSON 且仅输出 JSON，结构如下：
            {
              "opinion": "string",
              "accept": true
            }
            """;

    private static final String JSON_PATTERN = "(?s)\\{.*}";

    private final ChatModel chatModel;

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
            ChatResponse response = chatModel.chat(List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(new TextContent(payload))
            ));
            TagExpandDecisionDTO tagExpandDecisionDTO = parseDecision(extractAiText(response));
            return tagExpandDecisionDTO;
        } catch (Exception e) {
            log.warn("标签扩展判定失败，candidateRecallTag={}", candidateRecallTag, e);
            return new TagExpandDecisionDTO("模型判定异常，跳过召回标签", false);
        }
    }

    private String extractAiText(ChatResponse response) {
        if (response == null || response.aiMessage() == null) {
            return null;
        }
        return response.aiMessage().text();
    }

    private TagExpandDecisionDTO parseDecision(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new TagExpandDecisionDTO("模型返回为空，跳过召回标签", false);
        }
        try {
            String json = ReUtil.get(JSON_PATTERN, raw, 0);
            if (StrUtil.isBlank(json)) {
                json = raw;
            }
            String opinion = StrUtil.blankToDefault(JSONUtil.parseObj(json).getStr("opinion"), "未提供意见");
            Boolean accept = JSONUtil.parseObj(json).getBool("accept");
            if (accept == null) {
                String acceptText = JSONUtil.parseObj(json).getStr("accept");
                accept = "true".equalsIgnoreCase(StrUtil.trim(acceptText));
            }
            return new TagExpandDecisionDTO(opinion, Boolean.TRUE.equals(accept));
        } catch (Exception e) {
            log.warn("标签扩展判定解析失败，raw={}", raw, e);
            return new TagExpandDecisionDTO("解析失败，跳过召回标签", false);
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
