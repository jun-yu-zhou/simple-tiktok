package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.Follow;

import java.util.List;

public interface IFollowService extends IService<Follow> {
    boolean follow(Long userId, Long followId);

    boolean unfollow(Long userId, Long followId);

    boolean isFollowing(Long userId, Long followId);

    List<Long> listFollowingIds(Long userId);

    List<Long> listFansIds(Long userId);
}
