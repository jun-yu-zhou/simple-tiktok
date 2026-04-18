package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.example.simpletiktok.ai.ImageAuditAiService;
import com.example.simpletiktok.ai.TextAuditAiService;
import com.example.simpletiktok.ai.VideoAuditAiService;
import com.example.simpletiktok.result.AiReviewResult;
import com.example.simpletiktok.service.AiReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewServiceImpl implements AiReviewService {

    private final TextAuditAiService textAuditAiService;
    private final ImageAuditAiService imageAuditAiService;
    private final VideoAuditAiService videoAuditAiService;

    @Override
    public Double authImg(String imgUrl) {
        try {
            String normalizedUrl = normalizeRemoteUrl(imgUrl, "imgUrl");
            AiReviewResult result = imageAuditAiService.auditByUrl(normalizedUrl);
            log.info("图片审核结果: imgUrl={}, normalizedUrl={}, result={}", imgUrl, normalizedUrl, result);
            return safeScore(result);
        } catch (Exception e) {
            log.error("图片审核失败: imgUrl={}", imgUrl, e);
            return 100.0;
        }
    }

    @Override
    public Double authVideo(String videoUrl) {
        try {
            String normalizedUrl = normalizeRemoteUrl(videoUrl, "videoUrl");
            AiReviewResult result = videoAuditAiService.auditByUrl(normalizedUrl);
            log.info("视频审核结果: videoUrl={}, normalizedUrl={}, result={}", videoUrl, normalizedUrl, result);
            return safeScore(result);
        } catch (Exception e) {
            log.error("视频审核失败: videoUrl={}", videoUrl, e);
            return 100.0;
        }
    }

    @Override
    public Double authText(String text) {
        try {
            if (StrUtil.isBlank(text)) {
                return 100.0;
            }
            AiReviewResult result = textAuditAiService.audit(text);
            log.info("文本审核结果: text={}, result={}", text, result);
            return safeScore(result);
        } catch (Exception e) {
            log.error("文本审核失败: text={}", text, e);
            return 100.0;
        }
    }

    private Double safeScore(AiReviewResult result) {
        if (result == null || result.getTotalScore() == null) {
            return 100.0;
        }
        return NumberUtil.min(100.0, NumberUtil.max(0.0, result.getTotalScore()));
    }

    private String normalizeRemoteUrl(String url, String fieldName) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        String normalized = StrUtil.replace(StrUtil.trim(url), " ", "+");
        if (StrUtil.startWith(normalized, "http://")) {
            normalized = "https://" + StrUtil.removePrefix(normalized, "http://");
        }
        return normalized;
    }
}
