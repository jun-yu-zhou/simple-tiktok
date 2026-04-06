package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.service.ILabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TypeLabelSyncConsumer {

    private final ILabelService labelService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConfig.TYPE_LABEL_SYNC_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMqConfig.TYPE_LABEL_SYNC_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "true"),
            key = RabbitMqConfig.TYPE_LABEL_SYNC_ROUTING_KEY
    ))
    public void consume(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return;
        }
        labelService.syncAndVectorizeLabels(labels);
        log.info("type label sync finished, count={}", labels.size());
    }
}
