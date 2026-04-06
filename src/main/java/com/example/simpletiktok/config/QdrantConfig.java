package com.example.simpletiktok.config;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.qdrant", name = "enabled", havingValue = "true")
    public QdrantEmbeddingStore qdrantEmbeddingStore(QdrantProperties properties) {
        int port = properties.getPort() == null ? 6334 : properties.getPort();
        boolean useTls = properties.isUseTls();
        log.info(
                "Create QdrantEmbeddingStore -> host={}, port={}, useTls={}, collection={}",
                properties.getHost(),
                port,
                useTls,
                properties.getCollectionName()
        );

        QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(
                properties.getHost(),
                port,
                useTls,
                false
        );
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            grpcBuilder.withApiKey(properties.getApiKey().trim());
        }
        QdrantClient qdrantClient = new QdrantClient(grpcBuilder.build());

        return QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                .collectionName(properties.getCollectionName())
                .payloadTextKey(properties.getPayloadTextKey())
                .build();
    }
}
