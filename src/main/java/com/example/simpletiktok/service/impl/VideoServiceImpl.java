package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.config.RabbitMqConfig;
import com.example.simpletiktok.mapper.VideoMapper;
import com.example.simpletiktok.mq.VideoLikeModelProducer;
import com.example.simpletiktok.pojo.entity.Type;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.entity.VideoStar;
import com.example.simpletiktok.pojo.vo.HotRankVO;
import com.example.simpletiktok.service.IFavoritesService;
import com.example.simpletiktok.service.IFollowService;
import com.example.simpletiktok.service.ITypeService;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.service.IUserModelService;
import com.example.simpletiktok.service.IVideoService;
import com.example.simpletiktok.service.IVideoStarService;
import com.example.simpletiktok.util.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements IVideoService {

    private final IFavoritesService favoritesService;
    private final IFollowService followService;
    private final IUserService userService;
    private final ITypeService typeService;
    private final IUserModelService userModelService;
    private final IVideoStarService videoStarService;
    private final VideoLikeModelProducer videoLikeModelProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final AmqpAdmin amqpAdmin;
    private final RedissonClient redissonClient;

    private static final long HISTORY_BLOOM_EXPECTED_INSERTIONS = 100_000L;
    private static final double HISTORY_BLOOM_FALSE_PROBABILITY = 0.03D;

    @Override
    public List<HotRankVO> listHotRank() {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(RedisConstants.HOT_RANK, 0, -1);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderedVideoIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            Long videoId;
            try {
                videoId = Long.valueOf(tuple.getValue());
            } catch (NumberFormatException e) {
                continue;
            }
            orderedVideoIds.add(videoId);
            scoreMap.put(videoId, tuple.getScore());
        }
        if (orderedVideoIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Video> videos = this.listByIds(new LinkedHashSet<>(orderedVideoIds));
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));

        DecimalFormat format = new DecimalFormat("0.0");
        format.setRoundingMode(RoundingMode.HALF_UP);

        List<HotRankVO> result = new ArrayList<>();
        for (Long videoId : orderedVideoIds) {
            Video video = videoMap.get(videoId);
            if (video == null) {
                continue;
            }
            double hot = scoreMap.getOrDefault(videoId, 0D);
            HotRankVO vo = new HotRankVO();
            vo.setVideoId(videoId);
            vo.setCaption(video.getCaption());
            vo.setHot(hot);
            vo.setHotFormat(format.format(hot / 10000D) + "\u4e07");
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<Video> listHotVideo() {
        // 近三天热门抽样策略：今天 10 条，昨天 3 条，前天 2 条
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DATE);

        // 从三个“当日热门集合”中随机抽样视频 ID（Redis Set: hot:video:{day}）
        List<String> members = new ArrayList<>();
        appendRandomSetMembers(members, RedisConstants.HOT_VIDEO + today, 10);
        appendRandomSetMembers(members, RedisConstants.HOT_VIDEO + (today - 1), 3);
        appendRandomSetMembers(members, RedisConstants.HOT_VIDEO + (today - 2), 2);

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderedIds = new ArrayList<>();
        for (String member : members) {
            if (member == null || member.isBlank()) {
                continue;
            }
            try {
                orderedIds.add(Long.valueOf(member));
            } catch (NumberFormatException ignored) {
            }
        }
        if (orderedIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 按抽样后的 ID 顺序返回视频，避免 listByIds 打乱展示顺序
        List<Video> videos = this.listByIds(new LinkedHashSet<>(orderedIds));
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));
        List<Video> result = new ArrayList<>();
        for (Long id : orderedIds) {
            Video video = videoMap.get(id);
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    @Override
    public List<Video> listByUserIdVideo(Long userId, Long page, Long limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        long pageNo = page == null || page < 1 ? 1 : page;
        long pageSize = limit == null || limit < 1 ? 15 : limit;
        long offset = (pageNo - 1) * pageSize;
        return this.list(
                Wrappers.<Video>lambdaQuery()
                        .eq(Video::getUserId, userId)
                        .orderByDesc(Video::getGmtCreated)
                        .last("limit " + offset + "," + pageSize)
        );
    }

    @Override
    public Long countByUserIdVideo(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return this.count(Wrappers.<Video>lambdaQuery().eq(Video::getUserId, userId));
    }

    @Override
    public String getAuditQueueState() {
        try {
            Properties properties = amqpAdmin.getQueueProperties(RabbitMqConfig.VIDEO_AUDIT_QUEUE);
            if (properties == null || properties.isEmpty()) {
                return "审核队列不存在";
            }
            int ready = toInt(properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
            int consumers = toInt(properties.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
            if (ready <= 0) {
                return "审核队列空闲，待处理 0 条，消费者 " + consumers + " 个";
            }
            return "审核队列处理中，待处理 " + ready + " 条，消费者 " + consumers + " 个";
        } catch (Exception e) {
            return "审核队列状态获取失败";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer favoriteVideo(Long favoritesId, Long videoId, Long userId) {
        if (favoritesId == null || videoId == null || userId == null) {
            return -1;
        }
        if (!favoritesService.existsById(favoritesId, userId)) {
            return -1;
        }
        Video video = this.getById(videoId);
        if (video == null) {
            return -1;
        }
        boolean added = favoritesService.toggleFavorite(favoritesId, videoId, userId);
        long delta = added ? 1L : -1L;
        this.update(
                Wrappers.<Video>lambdaUpdate()
                        .eq(Video::getId, videoId)
                        .setSql("favorite_count = favorite_count + " + delta)
        );
        // 收藏/取消收藏同步调整兴趣模型，权重 2.0
        if (video.getLabels() != null && !video.getLabels().isEmpty()) {
            double interestDelta = added ? 2D : -2D;
            Map<String, Double> deltaMap = new LinkedHashMap<>();
            for (String label : video.getLabels()) {
                if (label == null) {
                    continue;
                }
                String labelName = label.trim();
                if (labelName.isEmpty()) {
                    continue;
                }
                deltaMap.merge(labelName, interestDelta, Double::sum);
            }
            if (!deltaMap.isEmpty()) {
                userModelService.applyLabelDelta(userId, deltaMap);
            }
        }
        return added ? 1 : 0;
    }

    @Override
    public List<Video> listVideoByFavorites(Long favoritesId, Long userId) {
        List<Long> videoIds = favoritesService.listVideoIds(favoritesId, userId);
        if (videoIds == null) {
            return null;
        }
        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.listByIds(videoIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer likeVideo(Long videoId, Long userId) {
        // 参数非法直接返回 -1，保持原有协议
        if (videoId == null || userId == null) {
            return -1;
        }

        // 视频不存在也返回 -1，避免后续无效写操作
        Video video = this.getById(videoId);
        if (video == null) {
            return -1;
        }

        // 1. 读取点赞关系，判断本次请求是“点赞”还是“取消点赞”
        VideoStar existing = videoStarService.getOne(
                Wrappers.<VideoStar>lambdaQuery()
                        .eq(VideoStar::getVideoId, videoId)
                        .eq(VideoStar::getUserId, userId)
        );
        boolean willLike = existing == null || existing.getIsDeleted() == null || existing.getIsDeleted() == 1;

        if (!willLike) {
            // 2. 取消点赞：把关系从已点赞(0)改成已取消(1)
            boolean changed = videoStarService.update(
                    Wrappers.<VideoStar>lambdaUpdate()
                            .eq(VideoStar::getId, existing.getId())
                            .eq(VideoStar::getIsDeleted, 0)
                            .set(VideoStar::getIsDeleted, 1)
            );
            // 未发生状态变更，说明当前就是未点赞，直接返回 0
            if (!changed) {
                return 0;
            }

            // 3. 状态变更成功后，再扣减点赞数；增加 >0 保护，避免出现负数
            this.update(
                    Wrappers.<Video>lambdaUpdate()
                            .eq(Video::getId, videoId)
                            .gt(Video::getLikeCount, 0)
                            .setSql("like_count = like_count - 1")
            );

            // 4. 同步扣减兴趣模型（取消点赞不走 MQ）
            userModelService.decreaseByVideoLabels(userId, video.getLabels());
            return 0;
        }

        // 5. 点赞：新关系直接插入，历史取消关系恢复为已点赞
        boolean changed = false;
        if (existing == null) {
            VideoStar star = new VideoStar();
            star.setVideoId(videoId);
            star.setUserId(userId);
            star.setIsDeleted(0);
            changed = videoStarService.save(star);
        }
        // 点赞关系表写入失败，说明当前就是已取消点赞，恢复为已点赞
        if (!changed) {
            // 从取消点赞的状态恢复为已点赞
            changed = videoStarService.update(
                    Wrappers.<VideoStar>lambdaUpdate()
                            .eq(VideoStar::getVideoId, videoId)
                            .eq(VideoStar::getUserId, userId)
                            .eq(VideoStar::getIsDeleted, 1)
                            .set(VideoStar::getIsDeleted, 0)
            );
        }

        // 未发生状态变更，说明当前就是已点赞（已有线程完成恢复点赞），直接返回 1
        if (!changed) {
            return 1;
        }

        // 6. 状态变更成功后，再执行点赞数 +1
        this.update(
                Wrappers.<Video>lambdaUpdate()
                        .eq(Video::getId, videoId)
                        .setSql("like_count = like_count + 1")
        );

        // 7. 点赞行为异步推送到兴趣模型链路；发送失败不影响主链路
        try {
            videoLikeModelProducer.send(userId, videoId);
        } catch (Exception e) {
            log.warn("点赞消息发送失败，userId={}, videoId={}", userId, videoId, e);
        }
        return 1;
    }

    @Override
    public boolean markUninterested(Long videoId, Long userId) {
        if (videoId == null || userId == null) {
            return false;
        }
        Video video = this.getById(videoId);
        if (video == null) {
            return false;
        }
        // 仅下调用户兴趣模型中“已存在”的标签，不新增任何标签，不走 MQ
        userModelService.decreaseExistingLabels(userId, video.getLabels(), 1D);
        return true;
    }

    @Override
    public boolean addHistory(Long videoId, Long userId) {
        if (videoId == null || userId == null) {
            return false;
        }
        Video video = this.getById(videoId);
        if (video == null) {
            return false;
        }

        String historyKey = RedisConstants.USER_HISTORY_VIDEO + userId;
        String bloomKey = RedisConstants.USER_HISTORY_BLOOM + userId;
        String member = String.valueOf(videoId);
        // 基于用户浏览历史 ZSet 去重：
        // add 返回 true 表示首次出现该 videoId；false 表示已存在（仅更新时间戳）
        Boolean firstView = stringRedisTemplate.opsForZSet().add(historyKey, member, System.currentTimeMillis());
        stringRedisTemplate.expire(historyKey, Duration.ofSeconds(RedisConstants.HISTORY_TIME));
        // 浏览记录同时写入布隆过滤器，用于推荐链路判重。
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(bloomKey);
        if (!bloomFilter.isExists()) {
            // 初始化
            bloomFilter.tryInit(HISTORY_BLOOM_EXPECTED_INSERTIONS, HISTORY_BLOOM_FALSE_PROBABILITY);
        }
        bloomFilter.add(member);
        if (!Boolean.TRUE.equals(firstView)) {
            return true;
        }

        this.update(
                Wrappers.<Video>lambdaUpdate()
                        .eq(Video::getId, videoId)
                        .setSql("view_count = view_count + 1")
        );
        // 浏览行为接入兴趣模型：仅首次观看加权，权重较低
        if (video.getLabels() != null && !video.getLabels().isEmpty()) {
            Map<String, Double> deltaMap = new LinkedHashMap<>();
            for (String label : video.getLabels()) {
                if (label == null) {
                    continue;
                }
                String labelName = label.trim();
                if (labelName.isEmpty()) {
                    continue;
                }
                deltaMap.merge(labelName, 0.2D, Double::sum);
            }
            if (!deltaMap.isEmpty()) {
                userModelService.applyLabelDelta(userId, deltaMap);
            }
        }
        return true;
    }

    @Override
    public LinkedHashMap<String, List<Video>> getHistory(Long userId, Long page, Long limit) {
        LinkedHashMap<String, List<Video>> result = new LinkedHashMap<>();
        if (userId == null) {
            return result;
        }
        long pageNo = page == null || page < 1 ? 1 : page;
        long pageSize = limit == null || limit < 1 ? 20 : limit;
        long start = (pageNo - 1) * pageSize;
        long end = start + pageSize - 1;

        String historyKey = RedisConstants.USER_HISTORY_VIDEO + userId;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(historyKey, start, end);
        if (tuples == null || tuples.isEmpty()) {
            return result;
        }

        List<Long> orderedVideoIds = new ArrayList<>();
        Map<Long, Long> scoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String value = tuple.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            Long videoId;
            try {
                videoId = Long.valueOf(value);
            } catch (NumberFormatException e) {
                continue;
            }
            orderedVideoIds.add(videoId);
            if (tuple.getScore() != null) {
                scoreMap.put(videoId, tuple.getScore().longValue());
            }
        }
        if (orderedVideoIds.isEmpty()) {
            return result;
        }

        List<Video> videos = this.listByIds(new LinkedHashSet<>(orderedVideoIds));
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Long videoId : orderedVideoIds) {
            Video video = videoMap.get(videoId);
            if (video == null) {
                continue;
            }
            long ts = scoreMap.getOrDefault(videoId, System.currentTimeMillis());
            LocalDate date = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
            String key = formatter.format(date);
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(video);
        }
        return result;
    }

    @Override
    public List<Video> searchByCaption(String search, Long page, Long limit) {
        if (search == null || search.isBlank()) {
            return Collections.emptyList();
        }
        long pageNo = page == null || page < 1 ? 1 : page;
        long pageSize = limit == null || limit < 1 ? 20 : limit;
        long offset = (pageNo - 1) * pageSize;
        return this.list(
                Wrappers.<Video>lambdaQuery()
                        .like(Video::getCaption, search.trim())
                        .eq(Video::getOpen, 1)
                        .eq(Video::getAuditStatus, 1)
                        .orderByDesc(Video::getGmtCreated)
                        .last("limit " + offset + "," + pageSize)
        );
    }

    @Override
    public List<Video> listByTypeId(Long typeId, Long page, Long limit) {
        if (typeId == null) {
            return Collections.emptyList();
        }
        long pageNo = page == null || page < 1 ? 1 : page;
        long pageSize = limit == null || limit < 1 ? 20 : limit;
        long offset = (pageNo - 1) * pageSize;
        String key = RedisConstants.SYSTEM_TYPE_STOCK + typeId;

        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (members != null && !members.isEmpty()) {
            List<Long> orderedTypeIds = new ArrayList<>();
            for (String member : members) {
                try {
                    orderedTypeIds.add(Long.valueOf(member));
                } catch (NumberFormatException ignored) {
                }
            }
            orderedTypeIds.sort((a, b) -> Long.compare(b, a));

            int from = (int) Math.min(offset, orderedTypeIds.size());
            int to = (int) Math.min(offset + pageSize, orderedTypeIds.size());
            if (from < to) {
                List<Long> pageVideoIds = orderedTypeIds.subList(from, to);
                List<Video> cacheVideos = this.list(
                        Wrappers.<Video>lambdaQuery()
                                .in(Video::getId, pageVideoIds)
                                .eq(Video::getOpen, 1)
                                .eq(Video::getAuditStatus, 1)
                );
                Map<Long, Video> videoMap = cacheVideos.stream()
                        .collect(Collectors.toMap(Video::getId, v -> v));
                List<Video> result = new ArrayList<>();
                for (Long id : pageVideoIds) {
                    Video video = videoMap.get(id);
                    if (video != null) {
                        result.add(video);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        List<Video> dbVideos = queryTypeVideosFromDb(typeId, offset, pageSize);
        if (!dbVideos.isEmpty()) {
            String[] videoIds = dbVideos.stream()
                    .map(Video::getId)
                    .filter(id -> id != null)
                    .map(String::valueOf)
                    .toArray(String[]::new);
            if (videoIds.length > 0) {
                stringRedisTemplate.opsForSet().add(key, videoIds);
            }
        }
        return dbVideos;
    }

    @Override
    public List<Video> listByUserIdOpenVideo(Long userId, Long page, Long limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        long pageNo = page == null || page < 1 ? 1 : page;
        long pageSize = limit == null || limit < 1 ? 20 : limit;
        long offset = (pageNo - 1) * pageSize;
        return this.list(
                Wrappers.<Video>lambdaQuery()
                        .eq(Video::getUserId, userId)
                        .eq(Video::getOpen, 1)
                        .eq(Video::getAuditStatus, 1)
                        .orderByDesc(Video::getGmtCreated)
                        .last("limit " + offset + "," + pageSize)
        );
    }

    @Override
    public List<Video> listSimilarVideo(Long videoId) {
        if (videoId == null) {
            return Collections.emptyList();
        }
        Video current = this.getById(videoId);
        if (current == null || current.getLabels() == null || current.getLabels().isEmpty()) {
            return Collections.emptyList();
        }

        // 将原标签重复一遍，提升原标签的抽样概率
        List<String> weightedLabels = new ArrayList<>();
        for (String label : current.getLabels()) {
            if (label == null) {
                continue;
            }
            String labelName = label.trim();
            if (labelName.isEmpty()) {
                continue;
            }
            weightedLabels.add(labelName);
            weightedLabels.add(labelName);
        }
        if (weightedLabels.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String labelName : weightedLabels) {
            String idValue = stringRedisTemplate.opsForSet().randomMember(RedisConstants.SYSTEM_STOCK + labelName);
            Long id = parseLong(idValue);
            if (id == null || id.equals(videoId)) {
                continue;
            }
            ids.add(id);
        }
        List<Video> result = listOpenAuditVideosByOrderedIds(new ArrayList<>(ids), 10);
        if (!result.isEmpty()) {
            return result;
        }

        // 兜底：按同分类取最新视频
        if (current.getTypeId() == null) {
            return Collections.emptyList();
        }
        List<Video> sameType = this.list(
                Wrappers.<Video>lambdaQuery()
                        .eq(Video::getTypeId, current.getTypeId())
                        .eq(Video::getOpen, 1)
                        .eq(Video::getAuditStatus, 1)
                        .ne(Video::getId, videoId)
                        .orderByDesc(Video::getGmtCreated)
                        .last("limit 10")
        );
        return sameType == null ? Collections.emptyList() : sameType;
    }

    @Override
    public List<Video> pushVideos(Long userId) {
        // 推送主链路：
        // 1. 登录用户先走兴趣模型抽样。
        // 2. 抽样为空时退化到游客标签随机抽样。
        // 3. 结果不足 10 条时再用最新公开视频兜底补齐。
        LinkedHashSet<Long> pushIds = new LinkedHashSet<>();
        if (userId != null) {
            pushIds.addAll(listVideoIdsByUserModel(userId));
        }
        // 兴趣模型为空或抽样结果为空时，走游客抽样。
        if (pushIds.isEmpty()) {
            pushIds.addAll(listVideoIdsByGuest(10));
        }

        List<Video> result = listOpenAuditVideosByOrderedIds(new ArrayList<>(pushIds), 10);
        if (result.size() >= 10) {
            return result;
        }

        // 兜底补齐，避免首页无内容
        Set<Long> exists = result.stream()
                .map(Video::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<Video> latest = this.list(
                Wrappers.<Video>lambdaQuery()
                        .eq(Video::getOpen, 1)
                        .eq(Video::getAuditStatus, 1)
                        .orderByDesc(Video::getGmtCreated)
                        .last("limit 30")
        );
        for (Video video : latest) {
            if (video.getId() == null || exists.contains(video.getId())) {
                continue;
            }
            result.add(video);
            exists.add(video.getId());
            if (result.size() >= 10) {
                break;
            }
        }
        return result;
    }

    @Override
    public List<Video> followFeed(Long userId, Long lastTime) {
        if (userId == null) {
            return Collections.emptyList();
        }
        // lastTime 为空或非正数时按首屏处理，避免传入 0 导致查空
        boolean firstPage = lastTime == null || lastTime <= 0;
        long maxScore = firstPage ? System.currentTimeMillis() : lastTime;
        long offset = firstPage ? 0 : 1;
        Set<String> ids = stringRedisTemplate.opsForZSet()
                .reverseRangeByScore(RedisConstants.IN_FOLLOW + userId, 0, maxScore, offset, 20);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> videoIds = new ArrayList<>();
        for (String id : ids) {
            try {
                videoIds.add(Long.valueOf(id));
            } catch (NumberFormatException ignored) {
            }
        }
        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Video> videos = this.listByIds(videoIds);
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));
        List<Video> result = new ArrayList<>();
        for (Long id : videoIds) {
            Video video = videoMap.get(id);
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    @Override
    public void initFollowFeed(Long userId) {
        if (userId == null) {
            return;
        }
        List<Long> followIds = followService.listFollowingIds(userId);
        if (followIds == null || followIds.isEmpty()) {
            return;
        }
        String inboxKey = RedisConstants.IN_FOLLOW + userId;
        long now = System.currentTimeMillis();
        long min = Instant.ofEpochMilli(now).minus(7, ChronoUnit.DAYS).toEpochMilli();
        Set<ZSetOperations.TypedTuple<String>> inboxLast =
                stringRedisTemplate.opsForZSet().rangeWithScores(inboxKey, -1, -1);
        if (inboxLast != null && !inboxLast.isEmpty()) {
            ZSetOperations.TypedTuple<String> tuple = inboxLast.iterator().next();
            if (tuple.getScore() != null) {
                min = tuple.getScore().longValue();
            }
        }

        for (Long followId : followIds) {
            String outboxKey = RedisConstants.OUT_FOLLOW + followId;
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeByScoreWithScores(outboxKey, min, now, 0, 50);
            if (tuples == null || tuples.isEmpty()) {
                continue;
            }
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String value = tuple.getValue();
                Double score = tuple.getScore();
                if (value == null || score == null) {
                    continue;
                }
                stringRedisTemplate.opsForZSet().add(inboxKey, value, score);
            }
        }
        stringRedisTemplate.expire(inboxKey, Duration.ofSeconds(RedisConstants.FEED_INBOX_TTL));
    }

    @Override
    public int shareVideoToFriend(Long videoId, Long userId, Long friendUserId) {
        // 返回码约定：1=首次分享成功；0=重复分享（幂等命中）；-1=参数/权限/状态错误
        if (videoId == null || userId == null || friendUserId == null) {
            return -1;
        }
        if (userId.equals(friendUserId)) {
            return -1;
        }
        boolean userFollowsFriend = followService.isFollowing(userId, friendUserId);
        boolean friendFollowsUser = followService.isFollowing(friendUserId, userId);
        if (!userFollowsFriend || !friendFollowsUser) {
            return -1;
        }
        Video video = this.getById(videoId);
        if (video == null || video.getAuditStatus() == null || video.getAuditStatus() != 1) {
            return -1;
        }

        // 统一分页基准：分享流也按视频发布时间(gmtCreated)作为 score
        long score = video.getGmtCreated() == null
                ? System.currentTimeMillis()
                : video.getGmtCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String inboxKey = RedisConstants.IN_FRIEND_SHARE + friendUserId;
        // ZSet add 语义：
        // true  -> member 原本不存在，本次首次写入（幂等成功）
        // false -> member 已存在，属于重复请求（幂等命中）
        Boolean added = stringRedisTemplate.opsForZSet().add(inboxKey, String.valueOf(videoId), score);
        stringRedisTemplate.expire(inboxKey, Duration.ofSeconds(RedisConstants.FEED_INBOX_TTL));

        if (Boolean.TRUE.equals(added)) {
            // 只有“首次写入收件箱”才累计分享数，避免重复点击造成无意义增长
            this.update(
                    Wrappers.<Video>lambdaUpdate()
                            .eq(Video::getId, videoId)
                            .setSql("share_count = share_count + 1")
            );
            return 1;
        }
        if (Boolean.FALSE.equals(added)) {
            return 0;
        }
        return -1;
    }

    @Override
    public List<Video> friendShareFeed(Long userId, Long lastTime) {
        if (userId == null) {
            return Collections.emptyList();
        }
        // lastTime 为空或非正数时按首屏处理，避免传入 0 导致查空
        boolean firstPage = lastTime == null || lastTime <= 0;
        long maxScore = firstPage ? System.currentTimeMillis() : lastTime;
        long offset = firstPage ? 0 : 1;
        Set<String> ids = stringRedisTemplate.opsForZSet()
                .reverseRangeByScore(RedisConstants.IN_FRIEND_SHARE + userId, 0, maxScore, offset, 20);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> videoIds = new ArrayList<>();
        for (String id : ids) {
            try {
                videoIds.add(Long.valueOf(id));
            } catch (NumberFormatException ignored) {
            }
        }
        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Video> videos = this.listByIds(videoIds);
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));
        List<Video> result = new ArrayList<>();
        for (Long id : videoIds) {
            Video video = videoMap.get(id);
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    @Override
    public void pushOutBoxFeed(Long userId, Long videoId, Long time) {
        if (userId == null || videoId == null || time == null) {
            return;
        }
        String key = RedisConstants.OUT_FOLLOW + userId;
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(videoId), time);
        // 发件箱按更长窗口保留，避免补拉时源数据过早丢失
        stringRedisTemplate.expire(key, Duration.ofSeconds(RedisConstants.FEED_OUTBOX_TTL));
    }

    @Override
    public List<Long> listFeedVideoIdsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return this.list(
                        Wrappers.<Video>lambdaQuery()
                                .select(Video::getId)
                                .eq(Video::getUserId, userId)
                                .eq(Video::getOpen, 1)
                                .eq(Video::getAuditStatus, 1)
                )
                .stream()
                .map(Video::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInBoxFeed(Long userId, List<Long> videoIds) {
        if (userId == null || videoIds == null || videoIds.isEmpty()) {
            return;
        }
        String key = RedisConstants.IN_FOLLOW + userId;
        stringRedisTemplate.opsForZSet().remove(key, videoIds.stream().map(String::valueOf).toArray());
    }

    @Override
    public void deleteOutBoxFeed(Long userId, List<Long> fans, Long videoId) {
        if (userId == null || videoId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().remove(RedisConstants.OUT_FOLLOW + userId, String.valueOf(videoId));
        if (fans == null || fans.isEmpty()) {
            return;
        }
        for (Long fanId : fans) {
            stringRedisTemplate.opsForZSet().remove(RedisConstants.IN_FOLLOW + fanId, String.valueOf(videoId));
        }
    }

    @Override
    public void pushSystemStockIn(Video video) {
        if (video == null || video.getId() == null || video.getLabels() == null || video.getLabels().isEmpty()) {
            return;
        }
        String videoId = String.valueOf(video.getId());
        for (String label : video.getLabels()) {
            if (label == null) {
                continue;
            }
            String labelName = label.trim();
            if (labelName.isEmpty()) {
                continue;
            }
            stringRedisTemplate.opsForSet().add(RedisConstants.SYSTEM_STOCK + labelName, videoId);
        }
    }

    @Override
    public void deleteSystemStockIn(Video video) {
        if (video == null || video.getId() == null || video.getLabels() == null || video.getLabels().isEmpty()) {
            return;
        }
        String videoId = String.valueOf(video.getId());
        for (String label : video.getLabels()) {
            if (label == null) {
                continue;
            }
            String labelName = label.trim();
            if (labelName.isEmpty()) {
                continue;
            }
            stringRedisTemplate.opsForSet().remove(RedisConstants.SYSTEM_STOCK + labelName, videoId);
        }
    }

    @Override
    public void pushSystemTypeStockIn(Video video) {
        if (video == null || video.getId() == null || video.getTypeId() == null) {
            return;
        }
        stringRedisTemplate.opsForSet().add(
                RedisConstants.SYSTEM_TYPE_STOCK + video.getTypeId(),
                String.valueOf(video.getId())
        );
    }

    @Override
    public void deleteSystemTypeStockIn(Video video) {
        if (video == null || video.getId() == null || video.getTypeId() == null) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(
                RedisConstants.SYSTEM_TYPE_STOCK + video.getTypeId(),
                String.valueOf(video.getId())
        );
    }

    @Override
    public boolean deleteVideo(Long videoId, Long userId) {
        if (videoId == null || userId == null) {
            return false;
        }
        Video video = this.getById(videoId);
        if (video == null || !userId.equals(video.getUserId())) {
            return false;
        }
        boolean removed = this.removeById(videoId);
        if (removed) {
            List<Long> fans = followService.listFansIds(userId);
            deleteOutBoxFeed(userId, fans, videoId);
            deleteSystemStockIn(video);
            deleteSystemTypeStockIn(video);
            stringRedisTemplate.opsForZSet().remove(RedisConstants.HOT_RANK, String.valueOf(videoId));
            // 删除视频后，同步清理近三天热门集合中的残留 ID
            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DATE);
            stringRedisTemplate.opsForSet().remove(RedisConstants.HOT_VIDEO + today, String.valueOf(videoId));
            stringRedisTemplate.opsForSet().remove(RedisConstants.HOT_VIDEO + (today - 1), String.valueOf(videoId));
            stringRedisTemplate.opsForSet().remove(RedisConstants.HOT_VIDEO + (today - 2), String.valueOf(videoId));
        }
        return removed;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void appendRandomSetMembers(List<String> target, String key, long count) {
        // 使用 Redis Set 的随机读取能力，控制每个日期 key 的抽样数量
        List<String> values = stringRedisTemplate.opsForSet().randomMembers(key, count);
        if (values == null || values.isEmpty()) {
            return;
        }
        target.addAll(values);
    }

    private List<Long> listVideoIdsByUserModel(Long userId) {
        // 读取用户兴趣模型（label -> weight），构造抽样概率池。
        Map<Object, Object> modelMap = stringRedisTemplate.opsForHash().entries(RedisConstants.USER_MODEL + userId);
        if (modelMap == null || modelMap.isEmpty()) {
            return Collections.emptyList();
        }
        String[] probabilityArray = initProbabilityArray(modelMap);
        if (probabilityArray.length == 0) {
            return Collections.emptyList();
        }
        Random random = new Random();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        // 按兴趣模型抽 8 次标签，再从标签库存随机取视频。
        // 取到的视频会做浏览历史判重，避免把已看视频再次推给用户。
        for (int i = 0; i < 8; i++) {
            String label = probabilityArray[random.nextInt(probabilityArray.length)];
            if (label == null || label.isBlank()) {
                continue;
            }
            String idValue = stringRedisTemplate.opsForSet().randomMember(RedisConstants.SYSTEM_STOCK + label);
            Long videoId = parseLong(idValue);
            if (videoId != null && !hasHistoryMark(videoId, userId)) {
                ids.add(videoId);
            }
        }
        // 按性别额外补一条偏好内容，同样走历史去重。
        User user = userService.getById(userId);
        Long extra = randomVideoIdBySex(user == null ? null : user.getSex());
        if (extra != null && !hasHistoryMark(extra, userId)) {
            ids.add(extra);
        }
        return new ArrayList<>(ids);
    }

    private List<Long> listVideoIdsByGuest(int sampleCount) {
        // 游客链路：基于开放分类标签随机取样，不做用户历史去重。
        List<String> labels = listOpenTypeLabels();
        if (labels.isEmpty()) {
            return Collections.emptyList();
        }
        Random random = new Random();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (int i = 0; i < sampleCount; i++) {
            String label = labels.get(random.nextInt(labels.size()));
            String idValue = stringRedisTemplate.opsForSet().randomMember(RedisConstants.SYSTEM_STOCK + label);
            Long videoId = parseLong(idValue);
            if (videoId != null) {
                ids.add(videoId);
            }
        }
        return new ArrayList<>(ids);
    }

    private List<String> listOpenTypeLabels() {
        List<Type> types = typeService.listOpen();
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> labels = new ArrayList<>();
        for (Type type : types) {
            List<String> typeLabels = type.getLabelNames();
            if (typeLabels == null || typeLabels.isEmpty()) {
                continue;
            }
            for (String label : typeLabels) {
                if (label != null && !label.isBlank()) {
                    labels.add(label.trim());
                }
            }
        }
        return labels;
    }

    /**
     * 将用户兴趣模型映射为“可随机下标抽取”的概率数组。
     * 1. 先把每个标签分数折算为正整数权重（至少为 1）。
     * 2. 再按权重重复写入数组，权重越大，标签在数组中出现次数越多。
     * 3. 调用方通过 random.nextInt(probabilityArray.length) 实现加权随机。
     */
    private String[] initProbabilityArray(Map<Object, Object> modelMap) {
        // 1. 基础校验：模型为空时直接返回空数组。
        if (modelMap == null || modelMap.isEmpty()) {
            return new String[0];
        }

        // 2. 将模型分数折算为整型权重，构建 label -> weight 映射。
        int size = modelMap.size();
        Map<String, Integer> probabilityMap = new LinkedHashMap<>();
        int total = 0;
        for (Map.Entry<Object, Object> entry : modelMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String label = String.valueOf(entry.getKey()).trim();
            if (label.isEmpty()) {
                continue;
            }
            double score = parseDouble(entry.getValue());
            // score 先转 int，小分值容易被截断。
            // 加上 size（标签总数）后，所有标签都有一个基础量。
            // 再做 weight / size 和 Math.max(1, ...)，确保最小权重至少是 1。
            int weight = (int) score + size;
            // 把权重按标签数做一次缩放，防止标签多时总权重膨胀太大，概率数组过长
            // 最外层max(1, xxx)为防御性代码，避免某个便签score为负数导致权重为0
            weight = Math.max(1, weight / Math.max(size, 1));
            probabilityMap.put(label, weight);
            total += weight;
        }

        // 3. 二次校验：总权重无效时返回空数组。
        if (total <= 0 || probabilityMap.isEmpty()) {
            return new String[0];
        }

        // 4. 生成概率数组：按权重重复写入标签。
        String[] probabilityArray = new String[total];
        int index = 0;
        for (Map.Entry<String, Integer> entry : probabilityMap.entrySet()) {
            for (int i = 0; i < entry.getValue() && index < probabilityArray.length; i++) {
                probabilityArray[index++] = entry.getKey();
            }
        }

        // 5. 返回给调用方做随机下标抽样。
        return probabilityArray;
    }

    private boolean hasHistoryMark(Long videoId, Long userId) {
        if (userId == null || videoId == null) {
            return false;
        }
        String bloomKey = RedisConstants.USER_HISTORY_BLOOM + userId;
        String historyKey = RedisConstants.USER_HISTORY_VIDEO + userId;
        String member = String.valueOf(videoId);

        // 先用布隆过滤器快速判断。
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(bloomKey);
        if (bloomFilter.isExists() && bloomFilter.contains(member)) {
            return true;
        }

        // 兜底检查历史记录，命中后回填到布隆过滤器，保证后续判重效率。
        Double score = stringRedisTemplate.opsForZSet().score(historyKey, member);
        if (score == null) {
            return false;
        }
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(HISTORY_BLOOM_EXPECTED_INSERTIONS, HISTORY_BLOOM_FALSE_PROBABILITY);
        }
        bloomFilter.add(member);
        return true;
    }

    private Long randomVideoIdBySex(Integer sex) {
        String label = (sex != null && sex == 1) ? "美女" : "宠物";
        String idValue = stringRedisTemplate.opsForSet().randomMember(RedisConstants.SYSTEM_STOCK + label);
        return parseLong(idValue);
    }

    private List<Video> listOpenAuditVideosByOrderedIds(List<Long> ids, int limit) {
        // 按输入 ID 顺序回表组装，只返回公开且审核通过的视频。
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<Long> orderedUnique = ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (orderedUnique.isEmpty()) {
            return new ArrayList<>();
        }
        List<Video> videos = this.list(
                Wrappers.<Video>lambdaQuery()
                        .in(Video::getId, orderedUnique)
                        .eq(Video::getOpen, 1)
                        .eq(Video::getAuditStatus, 1)
        );
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, v -> v));
        List<Video> result = new ArrayList<>(Math.min(limit, orderedUnique.size()));
        for (Long id : orderedUnique) {
            Video video = videoMap.get(id);
            if (video == null) {
                continue;
            }
            result.add(video);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private List<Video> queryTypeVideosFromDb(Long typeId, long offset, long pageSize) {
        return this.list(
                Wrappers.<Video>lambdaQuery()
                        .eq(Video::getTypeId, typeId)
                        .eq(Video::getOpen, 1)
                        .eq(Video::getAuditStatus, 1)
                        .orderByDesc(Video::getGmtCreated)
                        .last("limit " + offset + "," + pageSize)
        );
    }
}
