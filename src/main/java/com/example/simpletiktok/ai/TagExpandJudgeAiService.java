package com.example.simpletiktok.ai;

import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

import java.util.List;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        contentRetriever = "tagExpandQdrantRetriever"
)
public interface TagExpandJudgeAiService {

    @SystemMessage(AuditPrompts.TAG_EXPAND_JUDGE_PROMPT)
    @UserMessage("点赞视频标签列表：{{videoLabels}}")
    TagExpandDecisionDTO judge(@V("videoLabels") List<String> videoLabels);
}
