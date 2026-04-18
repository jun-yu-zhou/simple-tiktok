package com.example.simpletiktok.ai;

import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenChatModel")
public interface TagExpandJudgeAiService {

    @SystemMessage(AuditPrompts.TAG_EXPAND_JUDGE_PROMPT)
    TagExpandDecisionDTO judge(@UserMessage String tagSourceMapJson);
}
