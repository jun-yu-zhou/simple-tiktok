package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.LabelMapper;
import com.example.simpletiktok.pojo.entity.Label;
import com.example.simpletiktok.service.ILabelService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LabelServiceImpl extends ServiceImpl<LabelMapper, Label> implements ILabelService {

    private final EmbeddingModel embeddingModel;
    private final ObjectProvider<QdrantEmbeddingStore> qdrantStoreProvider;

    @Override
    public void syncAndVectorizeLabels(List<String> labelNames) {
        if (labelNames == null || labelNames.isEmpty()) {
            return;
        }

        // 归一化去重，保留首次出现的原始标签文案
        Map<String, String> normToRaw = new LinkedHashMap<>();
        for (String labelName : labelNames) {
            String norm = normalize(labelName);
            if (norm == null) {
                continue;
            }
            normToRaw.putIfAbsent(norm, labelName.trim());
        }
        if (normToRaw.isEmpty()) {
            return;
        }

        Set<String> normNames = normToRaw.keySet();
        List<Label> exists = this.list(
                Wrappers.<Label>lambdaQuery().in(Label::getNameNorm, normNames)
        );
        Set<String> existsNorm = exists.stream()
                .map(Label::getNameNorm)
                .collect(Collectors.toSet());

        List<Label> created = new ArrayList<>();
        for (Map.Entry<String, String> entry : normToRaw.entrySet()) {
            if (existsNorm.contains(entry.getKey())) {
                continue;
            }
            Label label = new Label();
            label.setName(entry.getValue());
            label.setNameNorm(entry.getKey());
            label.setVectorStatus(0);
            this.save(label);
            created.add(label);
        }
        if (created.isEmpty()) {
            return;
        }

        QdrantEmbeddingStore store = qdrantStoreProvider.getIfAvailable();
        if (store == null) {
            // 未启用Qdrant时，标签先保留待处理状态
            log.warn("Qdrant未启用，标签保持待向量化状态，count={}", created.size());
            return;
        }

        for (Label label : created) {
            try {
                // 使用LangChain4j EmbeddingModel生成标签向量
                Embedding embedding = embeddingModel.embed(label.getName()).content();
                if (embedding == null) {
                    markFailed(label.getId());
                    continue;
                }

                // Qdrant pointId使用UUID字符串
                String pointId = UUID.randomUUID().toString();
                // 写入向量数据库
                store.addAll(
                        List.of(pointId),
                        List.of(embedding),
                        List.of(TextSegment.from(label.getName()))
                );

                // 写入成功后更新状态
                Label update = new Label();
                update.setId(label.getId());
                update.setVectorStatus(1);
                update.setQdrantPointId(pointId);
                this.updateById(update);
            } catch (Exception e) {
                markFailed(label.getId());
                log.error("标签向量写入失败，labelId={}, label={}",
                        label.getId(), label.getName(), e);
            }
        }
    }

    @Override
    public String findOneSimilarLabel(String labelName, Set<String> excludes) {
        try {
            String norm = normalize(labelName);
            if (norm == null) {
                return null;
            }
            QdrantEmbeddingStore store = qdrantStoreProvider.getIfAvailable();
            if (store == null) {
                return null;
            }
            Embedding embedding = embeddingModel.embed(labelName).content();
            if (embedding == null) {
                return null;
            }
            // 检索器，maxResults不能唯一是有可能召回自己
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(5)
                    .minScore(0.55)
                    .build();
            // 召回文档
            EmbeddingSearchResult<TextSegment> result = store.search(request);
            if (result == null || result.matches() == null || result.matches().isEmpty()) {
                return null;
            }
            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                if (match == null || match.embeddingId() == null || match.embeddingId().isBlank()) {
                    continue;
                }
                // 根据QdrantPointId<->embeddingId搜索标签
                Label label = this.getOne(
                        Wrappers.<Label>lambdaQuery()
                                .eq(Label::getQdrantPointId, match.embeddingId())
                                .eq(Label::getVectorStatus, 1)
                                .last("limit 1")
                );
                if (label == null || label.getName() == null || label.getName().isBlank()) {
                    continue;
                }
                String candidate = label.getName().trim();
                String candidateNorm = normalize(candidate);
                if (candidateNorm == null || candidateNorm.equals(norm)) {
                    continue;
                }
                // excludes 中的标签统一跳过：包含原始标签及本轮已处理过的召回标签，避免重复扩展
                if (containsIgnoreBlank(excludes, candidateNorm)) {
                    continue;
                }
                return candidate;
            }
            return null;
        } catch (Exception e) {
            log.warn("相似标签检索失败，label={}", labelName, e);
            return null;
        }
    }

    private void markFailed(Long labelId) {
        Label update = new Label();
        update.setId(labelId);
        update.setVectorStatus(2);
        this.updateById(update);
    }

    private String normalize(String labelName) {
        if (labelName == null) {
            return null;
        }
        String value = labelName.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private boolean containsIgnoreBlank(Set<String> excludes, String normValue) {
        if (excludes == null || excludes.isEmpty() || normValue == null || normValue.isBlank()) {
            return false;
        }
        return excludes.stream()
                .map(this::normalize)
                .anyMatch(normValue::equals);
    }
}
