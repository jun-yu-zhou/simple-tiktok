package com.example.simpletiktok.mq;

import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.service.AiReviewService;
import com.example.simpletiktok.service.ILabelService;
import com.example.simpletiktok.service.IVideoService;
import com.example.simpletiktok.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class

VideoAuditConsumer {

    private static final double REVIEW_THRESHOLD = 66.0;

    private final AiReviewService aiReviewService;
    private final OssService ossService;
    private final IVideoService videoService;
    private final ILabelService labelService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConfig.VIDEO_AUDIT_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMqConfig.VIDEO_AUDIT_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "true"),
            key = RabbitMqConfig.VIDEO_AUDIT_ROUTING_KEY
    ))
    public void consume(Video payload) {
        Video dbVideo = videoService.getById(payload.getId());
        if (dbVideo == null || !Integer.valueOf(0).equals(dbVideo.getAuditStatus())) {
            return;
        }

        Double textScore = aiReviewService.authText(dbVideo.getCaption());
        Double imgScore = aiReviewService.authImg(ossService.getPublicUrl(dbVideo.getCoverFileName()));
        Double videoScore = aiReviewService.authVideo(ossService.getPublicUrl(dbVideo.getVideoFileName()));

        boolean pass = textScore < REVIEW_THRESHOLD
                && imgScore < REVIEW_THRESHOLD
                && videoScore < REVIEW_THRESHOLD;

        Video updateVideo = new Video();
        updateVideo.setId(dbVideo.getId());
        updateVideo.setAuditStatus(pass ? 1 : 2);
        updateVideo.setMsg(pass ? "审核通过" : "审核拒绝");
        videoService.updateById(updateVideo);

        if (pass) {
            if (dbVideo.getGmtCreated() != null) {
                long createdTime = dbVideo.getGmtCreated()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                // 写入发件箱
                videoService.pushOutBoxFeed(dbVideo.getUserId(), dbVideo.getId(), createdTime);
            }
            // 写入标签库
            videoService.pushSystemStockIn(dbVideo);
            // 写入分类库
            videoService.pushSystemTypeStockIn(dbVideo);
            // 审核通过后直接处理标签：去重入库 + 向量化写入 Qdrant
            labelService.syncAndVectorizeLabels(dbVideo.getLabels());
        }

        log.info("video audit finished, videoId={}, textScore={}, imgScore={}, videoScore={}, pass={}",
                dbVideo.getId(), textScore, imgScore, videoScore, pass);
    }
}
