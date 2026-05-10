package com.example.simpletiktok.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class TagExpandRagConfig {

    @Bean("tagExpandQdrantRetriever")
    public ContentRetriever tagExpandQdrantRetriever(
            ObjectProvider<QdrantEmbeddingStore> qdrantStoreProvider,
            EmbeddingModel embeddingModel
    ) {
        QdrantEmbeddingStore qdrantEmbeddingStore = qdrantStoreProvider.getIfAvailable();
        if (qdrantEmbeddingStore == null) {
            // Qdrant 未启用时返回空检索器，保证应用可正常启动。
            return query -> Collections.emptyList();
        }
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(qdrantEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(8)
                .minScore(0.55)
                .displayName("tag-expand-qdrant-retriever")
                .build();
    }
}
