package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TypeLabelSyncProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(List<String> labels) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.TYPE_LABEL_SYNC_EXCHANGE,
                RabbitMqConfig.TYPE_LABEL_SYNC_ROUTING_KEY,
                labels
        );
    }
}
