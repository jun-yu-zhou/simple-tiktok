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
        // 查数据库
        boolean exists = this.exists(
                Wrappers.<Follow>lambdaQuery()
                        .eq(Follow::getUserId, userId)
                        .eq(Follow::getFollowId, followId)
        );
        long now = System.currentTimeMillis();
        // 存在关注关系，补充redis
        if (exists) {
            stringRedisTemplate.opsForZSet().add(RedisConstants.USER_FOLLOW + userId, String.valueOf(followId), now);
            stringRedisTemplate.opsForZSet().add(RedisConstants.USER_FANS + followId, String.valueOf(userId), now);
            return true;
        }

        // 正常逻辑
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowId(followId);
        // 写数据库
        boolean ok = this.save(follow);
        // 双写redis
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
        // 删数据库里的关注关系
        boolean removed = this.remove(
                Wrappers.<Follow>lambdaQuery()
                        .eq(Follow::getUserId, userId)
                        .eq(Follow::getFollowId, followId)
        );
        // 删除 Redis 中“我的关注列表”里的这条记录
        stringRedisTemplate.opsForZSet().remove(RedisConstants.USER_FOLLOW + userId, String.valueOf(followId));
        // 删除 Redis 中“对方的粉丝列表”里的这条记录
        stringRedisTemplate.opsForZSet().remove(RedisConstants.USER_FANS + followId, String.valueOf(userId));
        // 只有数据库删除成功，才继续清理“关注收件箱”中的视频
        if (removed) {
            // 查询被取关用户发布过的、当前可见的视频ID
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
            // 如果查到了这些视频，就从当前用户的关注流收件箱中移除
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
