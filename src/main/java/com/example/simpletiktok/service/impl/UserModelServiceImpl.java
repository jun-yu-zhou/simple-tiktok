package com.example.simpletiktok.service.impl;

import com.example.simpletiktok.service.IUserModelService;
import com.example.simpletiktok.util.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserModelServiceImpl implements IUserModelService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void applyLabelDelta(Long userId, Map<String, Double> deltaMap) {
        if (userId == null || deltaMap == null || deltaMap.isEmpty()) {
            return;
        }
        String key = RedisConstants.USER_MODEL + userId;
        for (Map.Entry<String, Double> entry : deltaMap.entrySet()) {
            String label = normalize(entry.getKey());
            Double delta = entry.getValue();
            if (label == null || delta == null || delta == 0D) {
                continue;
            }
            Double score = stringRedisTemplate.opsForHash().increment(key, label, delta);
            // 分值小于等于 0 时删除该标签，避免兴趣模型膨胀
            if (score != null && score <= 0D) {
                stringRedisTemplate.opsForHash().delete(key, label);
            }
        }
    }

    @Override
    public void decreaseByVideoLabels(Long userId, List<String> labels) {
        if (userId == null || labels == null || labels.isEmpty()) {
            return;
        }
        Map<String, Double> deltaMap = new LinkedHashMap<>();
        for (String label : new LinkedHashSet<>(labels)) {
            String norm = normalize(label);
            if (norm == null) {
                continue;
            }
            deltaMap.put(norm, -1D);
        }
        applyLabelDelta(userId, deltaMap);
    }

    @Override
    public void initModelByLabels(Long userId, List<String> labels) {
        if (userId == null) {
            return;
        }
        String key = RedisConstants.USER_MODEL + userId;
        // 订阅初始化采用覆盖式重建，避免旧模型残留
        stringRedisTemplate.delete(key);
        if (labels == null || labels.isEmpty()) {
            return;
        }
        LinkedHashSet<String> labelSet = new LinkedHashSet<>();
        for (String label : labels) {
            String value = normalize(label);
            if (value != null) {
                labelSet.add(value);
            }
        }
        if (labelSet.isEmpty()) {
            return;
        }
        double score = 100D / labelSet.size();
        Map<String, String> modelMap = new LinkedHashMap<>();
        String scoreText = String.valueOf(score);
        for (String label : labelSet) {
            modelMap.put(label, scoreText);
        }
        stringRedisTemplate.opsForHash().putAll(key, modelMap);
    }

    @Override
    public boolean hasModel(Long userId) {
        if (userId == null) {
            return false;
        }
        String key = RedisConstants.USER_MODEL + userId;
        Long size = stringRedisTemplate.opsForHash().size(key);
        return size != null && size > 0;
    }

    @Override
    public void increaseMissingLabels(Long userId, List<String> labels, double delta) {
        if (userId == null || labels == null || labels.isEmpty() || delta <= 0D) {
            return;
        }
        String key = RedisConstants.USER_MODEL + userId;
        Set<String> fields = stringRedisTemplate.<String, String>opsForHash().keys(key);
        LinkedHashSet<String> exists = fields == null ? new LinkedHashSet<>() : new LinkedHashSet<>(fields);
        Map<String, Double> deltaMap = new LinkedHashMap<>();
        // 只给“模型中不存在”的标签加分，避免覆盖行为已形成的权重结构
        for (String label : labels) {
            String norm = normalize(label);
            if (norm == null || exists.contains(norm)) {
                continue;
            }
            deltaMap.put(norm, delta);
        }
        if (!deltaMap.isEmpty()) {
            applyLabelDelta(userId, deltaMap);
        }
    }

    @Override
    public void decreaseExistingLabels(Long userId, List<String> labels, double delta) {
        if (userId == null || labels == null || labels.isEmpty() || delta <= 0D) {
            return;
        }
        String key = RedisConstants.USER_MODEL + userId;
        Set<String> fields = stringRedisTemplate.<String, String>opsForHash().keys(key);
        if (fields == null || fields.isEmpty()) {
            return;
        }
        LinkedHashSet<String> exists = new LinkedHashSet<>(fields);
        Map<String, Double> deltaMap = new LinkedHashMap<>();
        // 只对“模型中已存在”的标签降权，避免把未出现标签误写入模型
        for (String label : labels) {
            String norm = normalize(label);
            if (norm == null || !exists.contains(norm)) {
                continue;
            }
            deltaMap.put(norm, -delta);
        }
        if (!deltaMap.isEmpty()) {
            applyLabelDelta(userId, deltaMap);
        }
    }

    private String normalize(String label) {
        if (label == null) {
            return null;
        }
        String value = label.trim();
        return value.isEmpty() ? null : value;
    }
}
