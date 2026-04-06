package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.VideoStar;

public interface IVideoStarService extends IService<VideoStar> {
    boolean toggleLike(Long videoId, Long userId);
}
