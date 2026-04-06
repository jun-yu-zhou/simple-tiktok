package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.pojo.dto.VideoLikeModelMessageDTO;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.service.ILabelService;
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
        Set<String> excludes = new LinkedHashSet<>(labels);
        for (String label : labels) {
            deltaMap.merge(label, BASE_LABEL_SCORE, Double::sum);
        }
        // 点赞异步补充一个相似标签，增强兴趣模型扩散能力
        for (String label : labels) {
            String similar = labelService.findOneSimilarLabel(label, excludes);
            if (similar == null || similar.isBlank()) {
                continue;
            }
            excludes.add(similar);
            deltaMap.merge(similar.trim(), SIMILAR_LABEL_SCORE, Double::sum);
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
