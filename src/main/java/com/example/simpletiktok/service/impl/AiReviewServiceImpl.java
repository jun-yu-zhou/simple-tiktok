package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.simpletiktok.exception.BizException;
import com.example.simpletiktok.result.AiReviewResult;
import com.example.simpletiktok.service.AiReviewService;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewServiceImpl implements AiReviewService {

    private static final String IMAGE_AUDIT_SYSTEM_PROMPT = """
            你是一个内容安全审核助手。
            请根据输入图片评估三个维度并打分（0-100，分数越高风险越高）：
            - pornScore（色情）
            - violenceScore（暴力）
            - politicalScore（涉政）

            输出必须是 JSON，且只输出 JSON：
            {
              "pornScore": 0,
              "violenceScore": 0,
              "politicalScore": 0,
              "totalScore": 0,
              "reason": "简短原因"
            }

            totalScore 计算规则：
            totalScore = pornScore * 0.4 + violenceScore * 0.3 + politicalScore * 0.3
            """;

    private static final String VIDEO_AUDIT_SYSTEM_PROMPT = """
            你是一个内容安全审核助手。
            请根据输入视频评估三个维度并打分（0-100，分数越高风险越高）：
            - pornScore（色情）
            - violenceScore（暴力）
            - politicalScore（涉政）

            输出必须是 JSON，且只输出 JSON：
            {
              "pornScore": 0,
              "violenceScore": 0,
              "politicalScore": 0,
              "totalScore": 0,
              "reason": "简短原因"
            }

            totalScore 计算规则：
            totalScore = pornScore * 0.4 + violenceScore * 0.3 + politicalScore * 0.3
            """;

    private static final String TEXT_AUDIT_SYSTEM_PROMPT = """
            你是一个内容安全审核助手。
            请根据输入文本评估三个维度并打分（0-100，分数越高风险越高）：
            - pornScore（色情）
            - violenceScore（暴力）
            - politicalScore（涉政）

            输出必须是 JSON，且只输出 JSON：
            {
              "pornScore": 0,
              "violenceScore": 0,
              "politicalScore": 0,
              "totalScore": 0,
              "reason": "简短原因"
            }

            totalScore 计算规则：
            totalScore = pornScore * 0.4 + violenceScore * 0.3 + politicalScore * 0.3
            """;

    private static final String JSON_PATTERN = "(?s)\\{.*}";

    private final ChatModel chatModel;

    @Override
    public Double authImg(String imgUrl) {
        try {
            String normalizedUrl = normalizeRemoteUrl(imgUrl, "imgUrl");
            ChatResponse response = chatModel.chat(List.of(
                    SystemMessage.from(IMAGE_AUDIT_SYSTEM_PROMPT),
                    UserMessage.from(
                            new TextContent("请审核这张图片，并按要求返回 JSON。"),
                            new ImageContent(normalizedUrl)
                    )
            ));

            AiReviewResult result = parseAiReviewResult(extractAiText(response));
            log.info("图片审核结果: imgUrl={}, normalizedUrl={}, result={}", imgUrl, normalizedUrl, result);
            return safeScore(result);
        } catch (BizException e) {
            log.warn("图片审核业务异常: imgUrl={}, msg={}", imgUrl, e.getMsg());
            return 100.0;
        } catch (Exception e) {
            log.error("图片审核失败: imgUrl={}", imgUrl, e);
            return 100.0;
        }
    }

    @Override
    public Double authVideo(String videoUrl) {
        try {
            String normalizedUrl = normalizeRemoteUrl(videoUrl, "videoUrl");
            ChatResponse response = chatModel.chat(List.of(
                    SystemMessage.from(VIDEO_AUDIT_SYSTEM_PROMPT),
                    UserMessage.from(
                            new TextContent("请审核这个视频，并按要求返回 JSON。"),
                            new VideoContent(normalizedUrl)
                    )
            ));

            AiReviewResult result = parseAiReviewResult(extractAiText(response));
            log.info("视频审核结果: videoUrl={}, normalizedUrl={}, result={}", videoUrl, normalizedUrl, result);
            return safeScore(result);
        } catch (BizException e) {
            log.warn("视频审核业务异常: videoUrl={}, msg={}", videoUrl, e.getMsg());
            return 100.0;
        } catch (Exception e) {
            log.error("视频审核失败: videoUrl={}", videoUrl, e);
            return 100.0;
        }
    }

    @Override
    public Double authText(String text) {
        try {
            if (StrUtil.isBlank(text)) {
                return 100.0;
            }
            ChatResponse response = chatModel.chat(List.of(
                    SystemMessage.from(TEXT_AUDIT_SYSTEM_PROMPT),
                    UserMessage.from(new TextContent(text))
            ));

            AiReviewResult result = parseAiReviewResult(extractAiText(response));
            log.info("文本审核结果: text={}, result={}", text, result);
            return safeScore(result);
        } catch (BizException e) {
            log.warn("文本审核业务异常: text={}, msg={}", text, e.getMsg());
            return 100.0;
        } catch (Exception e) {
            log.error("文本审核失败: text={}", text, e);
            return 100.0;
        }
    }

    private String extractAiText(ChatResponse response) {
        if (response == null || response.aiMessage() == null) {
            return null;
        }
        return response.aiMessage().text();
    }

    private AiReviewResult parseAiReviewResult(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new BizException("模型返回为空");
        }
        String json = ReUtil.get(JSON_PATTERN, raw, 0);
        if (StrUtil.isBlank(json)) {
            json = raw;
        }
        return JSONUtil.toBean(json, AiReviewResult.class);
    }

    private Double safeScore(AiReviewResult result) {
        if (result == null || result.getTotalScore() == null) {
            return 100.0;
        }
        return NumberUtil.min(100.0, NumberUtil.max(0.0, result.getTotalScore()));
    }

    private String normalizeRemoteUrl(String url, String fieldName) {
        if (StrUtil.isBlank(url)) {
            throw new BizException(fieldName + " 不能为空");
        }
        String normalized = StrUtil.replace(StrUtil.trim(url), " ", "+");
        if (StrUtil.startWith(normalized, "http://")) {
            normalized = "https://" + StrUtil.removePrefix(normalized, "http://");
        }
        return normalized;
    }
}
