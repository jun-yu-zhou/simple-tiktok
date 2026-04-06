package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.FollowMapper;
import com.example.simpletiktok.mapper.VideoMapper;
import com.example.simpletiktok.pojo.entity.Follow;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.service.IFollowService;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.util.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    private final IUserService userService;
    private final VideoMapper videoMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean follow(Long userId, Long followId) {
        if (userId == null || followId == null || userId.equals(followId)) {
            return false;
        }
        if (!existsActiveUser(userId) || !existsActiveUser(followId)) {
            return false;
        }
        boolean exists = this.exists(
                Wrappers.<Follow>lambdaQuery()
                        .eq(Follow::getUserId, userId)
                        .eq(Follow::getFollowId, followId)
        );
        long now = System.currentTimeMillis();
        if (exists) {
            stringRedisTemplate.opsForZSet().add(RedisConstants.USER_FOLLOW + userId, String.valueOf(followId), now);
            stringRedisTemplate.opsForZSet().add(RedisConstants.USER_FANS + followId, String.valueOf(userId), now);
            return true;
        }
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowId(followId);
        boolean ok = this.save(follow);
        if (ok) {
            stringRedisTemplate.opsForZSet().add(RedisConstants.USER_FOLLOW + userId, String.valueOf(followId), now);
            stringRedisTemplate.opsForZSet().add(RedisConstants.USER_FANS + followId, String.valueOf(userId), now);
        }
        return ok;
    }

    @Override
    public boolean unfollow(Long userId, Long followId) {
        if (userId == null || followId == null) {
            return false;
        }
        boolean removed = this.remove(
                Wrappers.<Follow>lambdaQuery()
                        .eq(Follow::getUserId, userId)
                        .eq(Follow::getFollowId, followId)
        );
        stringRedisTemplate.opsForZSet().remove(RedisConstants.USER_FOLLOW + userId, String.valueOf(followId));
        stringRedisTemplate.opsForZSet().remove(RedisConstants.USER_FANS + followId, String.valueOf(userId));
        if (removed) {
            List<Long> videoIds = videoMapper.selectList(
                            Wrappers.<Video>lambdaQuery()
                                    .select(Video::getId)
                                    .eq(Video::getUserId, followId)
                                    .eq(Video::getOpen, 1)
                                    .eq(Video::getAuditStatus, 1)
                    )
                    .stream()
                    .map(Video::getId)
                    .collect(Collectors.toList());
            if (!videoIds.isEmpty()) {
                stringRedisTemplate.opsForZSet().remove(RedisConstants.IN_FOLLOW + userId,
                        videoIds.stream().map(String::valueOf).toArray());
            }
        }
        return removed;
    }

    @Override
    public boolean isFollowing(Long userId, Long followId) {
        if (userId == null || followId == null) {
            return false;
        }
        return this.exists(
                Wrappers.<Follow>lambdaQuery()
                        .eq(Follow::getUserId, userId)
                        .eq(Follow::getFollowId, followId)
        );
    }

    @Override
    public List<Long> listFollowingIds(Long userId) {
        return this.list(
                        Wrappers.<Follow>lambdaQuery()
                                .select(Follow::getFollowId)
                                .eq(Follow::getUserId, userId)
                )
                .stream()
                .map(Follow::getFollowId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listFansIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return this.list(
                        Wrappers.<Follow>lambdaQuery()
                                .select(Follow::getUserId)
                                .eq(Follow::getFollowId, userId)
                )
                .stream()
                .map(Follow::getUserId)
                .collect(Collectors.toList());
    }

    private boolean existsActiveUser(Long userId) {
        User user = userService.getById(userId);
        return user != null && (user.getIsDeleted() == null || user.getIsDeleted() == 0);
    }
}
