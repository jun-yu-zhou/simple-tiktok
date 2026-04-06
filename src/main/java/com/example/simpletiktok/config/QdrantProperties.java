package com.example.simpletiktok.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.qdrant")
public class QdrantProperties {
    private boolean enabled;
    private String host;
    private Integer port;
    private boolean useTls;
    private String collectionName;
    private Integer dimension;
    private String payloadTextKey;
    private String apiKey;
}
