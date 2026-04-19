package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.pojo.dto.VideoLikeModelMessageDTO;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.vo.TagExpandResultVO;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoLikeModelConsumer {

    private final IVideoService videoService;
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

        // 尝试获取进行调整的标签结果，输入点赞视频的标签集合
        TagExpandResultVO result = tagExpandJudgeService.judge(labels);
        if (result == null || result.getLabelScoreMap() == null || result.getLabelScoreMap().isEmpty()) {
            return;
        }
        // 落入redis的用户兴趣模型
        userModelService.applyLabelDelta(message.getUserId(), result.getLabelScoreMap());
        log.info(
                "like model updated, userId={}, videoId={}, expandedAccepted={}, expandedLabel={}, opinion={}, labels={}",
                message.getUserId(),
                message.getVideoId(),
                Boolean.TRUE.equals(result.getExpandedAccepted()),
                result.getExpandedLabel(),
                result.getOpinion(),
                result.getLabelScoreMap().keySet()
        );
    }

    // 清洗、去重
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