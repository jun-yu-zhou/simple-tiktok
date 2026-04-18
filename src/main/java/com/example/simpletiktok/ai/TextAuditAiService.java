package com.example.simpletiktok.ai;

import com.example.simpletiktok.result.AiReviewResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenChatModel")
public interface TextAuditAiService {

    @SystemMessage(AuditPrompts.CONTENT_AUDIT_PROMPT)
    AiReviewResult audit(@UserMessage String text);
}
