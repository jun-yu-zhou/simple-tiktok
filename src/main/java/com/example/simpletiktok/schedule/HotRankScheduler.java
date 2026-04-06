package com.example.simpletiktok.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.service.IVideoService;
import com.example.simpletiktok.util.RedisConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class HotRankScheduler {

    private static final int TOP_K_SIZE = 10;
    private static final int BATCH_SIZE = 1000;
    private static final double DECAY_FACTOR = 0.011;

    private final IVideoService videoService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每小时重建一次热度排行榜（Redis ZSet: hot:rank）。
     * 算法要点：
     * 1. 使用小根堆 TopK，只保留热度最高的前 10 条。
     * 2. 使用主键 ID 递增分页（gt(id)+limit）扫描视频，避免深分页性能问题。
     * 3. 热度采用时间衰减方程：hot = weight * e^(-a*t)，a=0.011，t 为发布时间距今天数。
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void hotRank() {
        // 小根堆 TopK：只保留热度最高的 TOP_K_SIZE 条视频
        TopK<HotScore> topK = new TopK<>(
                TOP_K_SIZE,
                new PriorityQueue<>(TOP_K_SIZE, Comparator.comparingDouble(HotScore::getHot))
        );

        long lastId = 0L;
        while (true) {
            // ID 分页：按主键递增分批拉取，避免 offset 深分页性能问题
            List<Video> videos = videoService.list(
                    Wrappers.<Video>lambdaQuery()
                            .select(
                                    Video::getId,
                                    Video::getGmtCreated,
                                    Video::getViewCount,
                                    Video::getLikeCount,
                                    Video::getShareCount,
                                    Video::getFavoriteCount
                            )
                            .gt(Video::getId, lastId)
                            .eq(Video::getAuditStatus, 1)
                            .eq(Video::getOpen, 1)
                            .orderByAsc(Video::getId)
                            .last("limit " + BATCH_SIZE)
            );
            if (videos == null || videos.isEmpty()) {
                break;
            }
            for (Video video : videos) {
                if (video.getId() == null) {
                    continue;
                }
                topK.add(new HotScore(video.getId(), calculateHot(video)));
            }
            lastId = videos.get(videos.size() - 1).getId();
        }

        List<HotScore> result = topK.getDescList();
        stringRedisTemplate.delete(RedisConstants.HOT_RANK);
        for (HotScore score : result) {
            stringRedisTemplate.opsForZSet().add(
                    RedisConstants.HOT_RANK,
                    String.valueOf(score.getVideoId()),
                    score.getHot()
            );
        }
    }

    @PostConstruct
    public void initHotRankOnStartup() {
        hotRank();
        hotVideo();
    }

    private double calculateHot(Video video) {
        double share = value(video.getShareCount());
        double view = value(video.getViewCount()) * 0.8;
        double like = value(video.getLikeCount());
        double favorite = value(video.getFavoriteCount()) * 1.5;
        double randomWeight = (int) ((Math.random() * 9 + 1) * 100000) / 1_000_000.0;
        double ageDays = video.getGmtCreated() == null
                ? 0
                : Math.max(0, ChronoUnit.DAYS.between(video.getGmtCreated(), LocalDateTime.now()));
        // 时间衰减方程：hot = weight * e^(-a*t)，a=0.011，t 为发布时间距今天数
        return (share + view + like + favorite + randomWeight) * Math.exp(-DECAY_FACTOR * ageDays);
    }

    private double value(Long num) {
        return num == null ? 0D : num.doubleValue();
    }

    /**
     * 每 3 小时将热榜回填到“当日热门集合”（Redis Set: hot:video:{day}）。
     * 说明：
     * 1. 从 hot:rank 读取前 30 条视频 ID，写入 hot:video:{today}。
     * 2. 集合过期时间为 3 天，用于配合“近三天 10/3/2 随机”读取策略。
     */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void hotVideo() {
        int today = LocalDate.now().getDayOfMonth();
        String key = RedisConstants.HOT_VIDEO + today;

        // 取热榜前 30 作为当日热门候选
        Set<String> topIds = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisConstants.HOT_RANK, 0, 29);
        if (topIds == null || topIds.isEmpty()) {
            return;
        }

        stringRedisTemplate.delete(key);
        stringRedisTemplate.opsForSet().add(key, topIds.toArray(String[]::new));
        stringRedisTemplate.expire(key, java.time.Duration.ofDays(3));
    }
}
