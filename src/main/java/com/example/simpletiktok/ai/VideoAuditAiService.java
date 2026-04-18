package com.example.simpletiktok.ai;

import com.example.simpletiktok.result.AiReviewResult;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

import java.util.List;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenChatModel")
public interface VideoAuditAiService {

    @SystemMessage(AuditPrompts.CONTENT_AUDIT_PROMPT)
    AiReviewResult audit(List<Content> contents);

    default AiReviewResult auditByUrl(String videoUrl) {
        return audit(List.of(
                new TextContent("请审核这个视频，并按要求返回 JSON。"),
                VideoContent.from(videoUrl)
        ));
    }
}
