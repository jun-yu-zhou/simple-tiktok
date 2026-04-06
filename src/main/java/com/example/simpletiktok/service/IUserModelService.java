package com.example.simpletiktok.service;

import java.util.List;
import java.util.Map;

public interface IUserModelService {
    void applyLabelDelta(Long userId, Map<String, Double> deltaMap);

    void decreaseByVideoLabels(Long userId, List<String> labels);

    void initModelByLabels(Long userId, List<String> labels);

    boolean hasModel(Long userId);

    void increaseMissingLabels(Long userId, List<String> labels, double delta);

    void decreaseExistingLabels(Long userId, List<String> labels, double delta);
}
