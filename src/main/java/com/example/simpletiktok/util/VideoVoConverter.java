package com.example.simpletiktok.util;

import cn.hutool.core.bean.BeanUtil;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.vo.VideoVO;
import com.example.simpletiktok.service.IFollowService;
import com.example.simpletiktok.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VideoVoConverter {

    private final IUserService userService;
    private final IFollowService followService;

    public VideoVO toVideoVO(Video video) {
        if (video == null) {
            return null;
        }
        VideoVO vo = BeanUtil.copyProperties(video, VideoVO.class);
        if (video.getUserId() != null) {
            User author = userService.getById(video.getUserId());
            if (author != null) {
                vo.setUserNickName(author.getNickName());
                vo.setUserAvatar(author.getAvatar());
            }
        }
        Long createdTime = toEpochMilli(video.getGmtCreated());
        Long updatedTime = toEpochMilli(video.getGmtUpdated());
        vo.setGmtCreated(createdTime);
        vo.setGmtUpdated(updatedTime);
        vo.setFollowedAuthor(resolveFollowedAuthor(video.getUserId()));
        return vo;
    }

    public List<VideoVO> toVideoVOList(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }
        return videos.stream().map(this::toVideoVO).collect(Collectors.toList());
    }

    private Boolean resolveFollowedAuthor(Long authorId) {
        User current = UserHolder.get();
        if (current == null || authorId == null) {
            return false;
        }
        if (current.getId().equals(authorId)) {
            return true;
        }
        return followService.isFollowing(current.getId(), authorId);
    }

    private Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
