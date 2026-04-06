package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.pojo.dto.VideoLikeModelMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoLikeModelProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(Long userId, Long videoId) {
        VideoLikeModelMessageDTO dto = new VideoLikeModelMessageDTO();
        dto.setUserId(userId);
        dto.setVideoId(videoId);
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.VIDEO_LIKE_MODEL_EXCHANGE,
                RabbitMqConfig.VIDEO_LIKE_MODEL_ROUTING_KEY,
                dto
        );
    }
}
