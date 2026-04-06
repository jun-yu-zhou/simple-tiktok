package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.VideoStarMapper;
import com.example.simpletiktok.pojo.entity.VideoStar;
import com.example.simpletiktok.service.IVideoStarService;
import org.springframework.stereotype.Service;

@Service
public class VideoStarServiceImpl extends ServiceImpl<VideoStarMapper, VideoStar> implements IVideoStarService {
    @Override
    public boolean toggleLike(Long videoId, Long userId) {
        VideoStar existing = this.getOne(
                Wrappers.<VideoStar>lambdaQuery()
                        .eq(VideoStar::getVideoId, videoId)
                        .eq(VideoStar::getUserId, userId)
        );
        if (existing == null) {
            VideoStar star = new VideoStar();
            star.setVideoId(videoId);
            star.setUserId(userId);
            star.setIsDeleted(0);
            return this.save(star);
        }
        if (existing.getIsDeleted() != null && existing.getIsDeleted() == 0) {
            existing.setIsDeleted(1);
        } else {
            existing.setIsDeleted(0);
        }
        return this.updateById(existing);
    }
}
