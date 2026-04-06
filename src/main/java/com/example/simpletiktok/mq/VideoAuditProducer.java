package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.pojo.entity.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoAuditProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(Video video) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.VIDEO_AUDIT_EXCHANGE,
                RabbitMqConfig.VIDEO_AUDIT_ROUTING_KEY,
                video
        );
    }
}
