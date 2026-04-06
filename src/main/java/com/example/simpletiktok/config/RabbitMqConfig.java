package com.example.simpletiktok.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String VIDEO_AUDIT_QUEUE = "video.audit.queue";
    public static final String VIDEO_AUDIT_EXCHANGE = "video.audit.exchange";
    public static final String VIDEO_AUDIT_ROUTING_KEY = "video.audit";
    public static final String VIDEO_LIKE_MODEL_QUEUE = "video.like.model.queue";
    public static final String VIDEO_LIKE_MODEL_EXCHANGE = "video.like.model.exchange";
    public static final String VIDEO_LIKE_MODEL_ROUTING_KEY = "video.like.model";
    public static final String TYPE_LABEL_SYNC_QUEUE = "type.label.sync.queue";
    public static final String TYPE_LABEL_SYNC_EXCHANGE = "type.label.sync.exchange";
    public static final String TYPE_LABEL_SYNC_ROUTING_KEY = "type.label.sync";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
