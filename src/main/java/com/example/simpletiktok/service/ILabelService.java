package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.Label;

import java.util.List;
import java.util.Set;

public interface ILabelService extends IService<Label> {
    void syncAndVectorizeLabels(List<String> labelNames);

    String findOneSimilarLabel(String labelName, Set<String> excludes);
}
