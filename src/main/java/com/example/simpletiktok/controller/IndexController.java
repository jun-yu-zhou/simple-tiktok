package com.example.simpletiktok.controller;

import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.vo.HotRankVO;
import com.example.simpletiktok.pojo.vo.VideoVO;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.service.IVideoService;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import com.example.simpletiktok.util.VideoVoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexController {

    private final IUserService userService;
    private final IVideoService videoService;
    private final VideoVoConverter videoVoConverter;

    /**
     * 获取当前用户搜索历史
     */
    @GetMapping("/search/history")
    public R<?> searchHistory() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        return R.ok().data(userService.searchHistory(current.getId()));
    }

    /**
     * 清空当前用户搜索历史
     */
    @DeleteMapping("/search/history")
    public R<?> deleteSearchHistory() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        userService.deleteSearchHistory(current.getId());
        return R.ok().message("清空成功");
    }

    /**
     * 按文案搜索视频
     */
    @GetMapping("/search")
    public R<?> search(@RequestParam String search,
                       @RequestParam(required = false) Long page,
                       @RequestParam(required = false) Long limit) {
        if (search == null || search.isBlank()) {
            return R.ok().data(Collections.emptyList());
        }
        User current = UserHolder.get();
        if (current != null) {
            userService.addSearchHistory(current.getId(), search);
        }
        List<Video> videos = videoService.searchByCaption(search, page, limit);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 获取热视频排行榜
     */
    @GetMapping("/video/hot/rank")
    public R<?> hotRank() {
        List<HotRankVO> list = videoService.listHotRank();
        return R.ok().data(list);
    }

    /**
     * 获取热门视频列表
     */
    @GetMapping("/video/hot")
    public R<?> hotVideos() {
        List<Video> videos = videoService.listHotVideo();
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 兴趣推送视频列表
     */
    @GetMapping("/pushVideos")
    public R<?> pushVideos() {
        User current = UserHolder.get();
        Long userId = current == null ? null : current.getId();
        List<Video> videos = videoService.pushVideos(userId);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }
}
