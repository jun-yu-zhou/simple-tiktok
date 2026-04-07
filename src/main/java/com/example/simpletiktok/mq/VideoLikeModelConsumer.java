package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.pojo.dto.TagExpandDecisionDTO;
import com.example.simpletiktok.pojo.dto.VideoLikeModelMessageDTO;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.service.ILabelService;
import com.example.simpletiktok.service.ITagExpandJudgeService;
import com.example.simpletiktok.service.IUserModelService;
import com.example.simpletiktok.service.IVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoLikeModelConsumer {

    private static final double BASE_LABEL_SCORE = 1D;
    private static final double SIMILAR_LABEL_SCORE = 0.4D;

    private final IVideoService videoService;
    private final ILabelService labelService;
    private final ITagExpandJudgeService tagExpandJudgeService;
    private final IUserModelService userModelService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConfig.VIDEO_LIKE_MODEL_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMqConfig.VIDEO_LIKE_MODEL_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "true"),
            key = RabbitMqConfig.VIDEO_LIKE_MODEL_ROUTING_KEY
    ))
    public void consume(VideoLikeModelMessageDTO message) {
        if (message == null || message.getUserId() == null || message.getVideoId() == null) {
            return;
        }
        Video video = videoService.getById(message.getVideoId());
        if (video == null || video.getLabels() == null || video.getLabels().isEmpty()) {
            return;
        }
        List<String> labels = normalizeLabels(video.getLabels());
        if (labels.isEmpty()) {
            return;
        }

        Map<String, Double> deltaMap = new LinkedHashMap<>();
        Map<String, String> baseTagSourceMap = new LinkedHashMap<>();
        Set<String> excludes = new LinkedHashSet<>(labels);
        for (String label : labels) {
            deltaMap.merge(label, BASE_LABEL_SCORE, Double::sum);
            baseTagSourceMap.put(label, "source_video");
        }
        // 点赞异步补充相似标签；命中一个可接受的召回标签后立即结束
        boolean recallAccepted = false;
        for (String label : labels) {
            // 结束
            if (recallAccepted) {
                break;
            }
            String similar = labelService.findOneSimilarLabel(label, excludes);
            if (similar == null || similar.isBlank()) {
                continue;
            }
            String similarTag = similar.trim();
            if (similarTag.isEmpty()) {
                continue;
            }
            excludes.add(similarTag);

            Map<String, String> tagSourceMap = new LinkedHashMap<>(baseTagSourceMap);
            tagSourceMap.put(similarTag, "source_recall");
            TagExpandDecisionDTO decision = tagExpandJudgeService.judge(tagSourceMap);
            if (decision != null && Boolean.TRUE.equals(decision.getAccept())) {
                deltaMap.merge(similarTag, SIMILAR_LABEL_SCORE, Double::sum);
                // 找到标签适合写入，结束
                recallAccepted = true;
            }
            log.info("like model tag decision, userId={}, videoId={}, tag={}, accept={}, opinion={}",
                    message.getUserId(),
                    message.getVideoId(),
                    similarTag,
                    decision != null && Boolean.TRUE.equals(decision.getAccept()),
                    decision == null ? "" : decision.getOpinion());
        }
        userModelService.applyLabelDelta(message.getUserId(), deltaMap);
        log.info("like model updated, userId={}, videoId={}, labels={}",
                message.getUserId(), message.getVideoId(), deltaMap.keySet());
    }

    private List<String> normalizeLabels(List<String> labels) {
        Set<String> set = new LinkedHashSet<>();
        for (String label : labels) {
            if (label == null) {
                continue;
            }
            String value = label.trim();
            if (!value.isEmpty()) {
                set.add(value);
            }
        }
        return new ArrayList<>(set);
    }
}
