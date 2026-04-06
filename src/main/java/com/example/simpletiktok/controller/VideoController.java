package com.example.simpletiktok.controller;

import cn.hutool.core.bean.BeanUtil;
import com.example.simpletiktok.exception.BizException;
import com.example.simpletiktok.mq.VideoAuditProducer;
import com.example.simpletiktok.limit.Limit;
import com.example.simpletiktok.pojo.dto.VideoSaveDTO;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.vo.FavoriteSelectStateVO;
import com.example.simpletiktok.pojo.vo.VideoPageVO;
import com.example.simpletiktok.pojo.vo.VideoVO;
import com.example.simpletiktok.service.IFavoritesService;
import com.example.simpletiktok.service.IVideoService;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.RedisConstants;
import com.example.simpletiktok.util.UserHolder;
import com.example.simpletiktok.util.VideoVoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoService videoService;
    private final IFavoritesService favoritesService;
    private final VideoVoConverter videoVoConverter;
    private final VideoAuditProducer videoAuditProducer;

    /**
     * 保存视频基础信息
     */
    @PostMapping("/save")
    @Limit(limit = 5, time = 3600, key = RedisConstants.VIDEO_UPLOAD_LIMIT, msg = "1小时内最多上传5个视频")
    public R<?> save(@RequestBody VideoSaveDTO dto) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        // 有 id 表示编辑已有稿件：按视频 id 更新，而不是新建一条记录
        if (dto.getId() != null) {
            Video existing = videoService.getById(dto.getId());
            if (existing == null) {
                throw new BizException("视频不存在");
            }
            if (!current.getId().equals(existing.getUserId())) {
                throw new BizException("无权修改该视频");
            }
            applySaveDto(existing, dto);
            existing.setAuditStatus(0);
            existing.setMsg(null);
            // 落库
            videoService.updateById(existing);
            // 重新审核
            videoAuditProducer.send(existing);
            return R.ok().message("修改成功").data(existing.getId());
        }

        Video video = BeanUtil.copyProperties(dto, Video.class);
        video.setUserId(current.getId());
        if (video.getOpen() == null) {
            video.setOpen(1);
        }
        video.setAuditStatus(0);
        video.setViewCount(0L);
        video.setLikeCount(0L);
        video.setShareCount(0L);
        video.setFavoriteCount(0L);
        videoService.save(video);
        videoAuditProducer.send(video);
        return R.ok().message("发布成功").data(video.getId());
    }

    private void applySaveDto(Video target, VideoSaveDTO dto) {
        if (dto.getCaption() != null) {
            target.setCaption(dto.getCaption());
        }
        if (dto.getVideoFileName() != null) {
            target.setVideoFileName(dto.getVideoFileName());
        }
        if (dto.getCoverFileName() != null) {
            target.setCoverFileName(dto.getCoverFileName());
        }
        if (dto.getLabels() != null) {
            target.setLabels(dto.getLabels());
        }
        if (dto.getOpen() != null) {
            target.setOpen(dto.getOpen());
        }
        if (dto.getDuration() != null) {
            target.setDuration(dto.getDuration());
        }
        if (dto.getTypeId() != null) {
            target.setTypeId(dto.getTypeId());
        }
    }

    /**
     * 根据 ID 获取视频详情
     */
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        Video video = videoService.getById(id);
        if (video == null) {
            return R.error().message("视频不存在");
        }
        return R.ok().data(videoVoConverter.toVideoVO(video));
    }

    /**
     * 根据分类查询视频列表
     */
    @GetMapping("/type/{typeId}")
    public R<?> listByType(@PathVariable Long typeId,
                           @RequestParam(required = false) Long page,
                           @RequestParam(required = false) Long limit) {
        List<Video> videos = videoService.listByTypeId(typeId, page, limit);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 获取指定作者的公开视频列表
     */
    @GetMapping("/user/open/{userId}")
    public R<?> listOpenByUser(@PathVariable Long userId,
                               @RequestParam(required = false) Long page,
                               @RequestParam(required = false) Long limit) {
        List<Video> videos = videoService.listByUserIdOpenVideo(userId, page, limit);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 获取相似视频推荐
     */
    @GetMapping("/similar")
    public R<?> similarVideos(@RequestParam Long videoId) {
        List<Video> videos = videoService.listSimilarVideo(videoId);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 获取当前用户的视频稿件列表（分页）
     */
    @GetMapping
    public R<?> listByUser(@RequestParam(required = false) Long page,
                           @RequestParam(required = false) Long limit) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        long pageNo = page == null || page < 1 ? 1L : page;
        long pageSize = limit == null || limit < 1 ? 15L : limit;
        List<Video> list = videoService.listByUserIdVideo(current.getId(), pageNo, pageSize);
        Long total = videoService.countByUserIdVideo(current.getId());

        VideoPageVO result = new VideoPageVO();
        result.setPage(pageNo);
        result.setLimit(pageSize);
        result.setTotal(total == null ? 0L : total);
        result.setRecords(videoVoConverter.toVideoVOList(list));
        return R.ok().data(result);
    }

    /**
     * 获取指定收藏夹下的视频列表
     */
    @GetMapping("/favorites/{favoritesId}")
    public R<?> listVideoByFavorites(@PathVariable Long favoritesId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        List<Video> list = videoService.listVideoByFavorites(favoritesId, current.getId());
        if (list == null) {
            return R.error().message("收藏夹不存在");
        }
        List<VideoVO> result = videoVoConverter.toVideoVOList(list);
        return R.ok().data(result);
    }

    /**
     * 收藏/取消收藏视频
     */
    @PostMapping("/favorites/{favoritesId}/{videoId}")
    public R<?> favoriteVideo(@PathVariable Long favoritesId, @PathVariable Long videoId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        Integer result = videoService.favoriteVideo(favoritesId, videoId, current.getId());
        if (result == null || result < 0) {
            return R.error().message("收藏失败");
        }
        String msg = result == 1 ? "已收藏" : "取消收藏";
        return R.ok().message(msg);
    }

    /**
     * 查询“当前视频在各收藏夹中的状态”，用于收藏弹窗展示。
     */
    @GetMapping("/favorites/state/{videoId}")
    public R<?> favoriteStates(@PathVariable Long videoId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        List<FavoriteSelectStateVO> states = favoritesService.listFavoriteStates(current.getId(), videoId);
        return R.ok().data(states);
    }

    /**
     * 点赞/取消点赞视频
     */
    @PostMapping("/like/{videoId}")
    public R<?> likeVideo(@PathVariable Long videoId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        Integer result = videoService.likeVideo(videoId, current.getId());
        if (result == null || result < 0) {
            return R.error().message("点赞失败");
        }
        String msg = result == 1 ? "已点赞" : "取消点赞";
        return R.ok().message(msg);
    }

    /**
     * 标记“不感兴趣”：根据视频标签下调用户兴趣模型中已有标签
     */
    @PostMapping("/uninterested/{videoId}")
    public R<?> uninterested(@PathVariable Long videoId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        boolean ok = videoService.markUninterested(videoId, current.getId());
        return ok ? R.ok().message("已降低该视频相关兴趣") : R.error().message("操作失败");
    }

    /**
     * 分享到好友收件箱（仅互相关注）
     */
    @PostMapping("/share/friend/{videoId}/{friendUserId}")
    public R<?> shareVideoToFriend(@PathVariable Long videoId, @PathVariable Long friendUserId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        int result = videoService.shareVideoToFriend(videoId, current.getId(), friendUserId);
        // 与服务层返回码保持一致，前端据此决定是否本地 +1
        if (result == 1) {
            return R.ok().message("分享成功").data(true);
        }
        if (result == 0) {
            return R.ok().message("已分享过该视频").data(false);
        }
        return R.error().message("分享失败");
    }

    /**
     * 添加浏览记录（同一用户 5 天内重复观看同一视频不重复计数）
     */
    @PostMapping("/history/{videoId}")
    public R<?> addHistory(@PathVariable Long videoId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        boolean ok = videoService.addHistory(videoId, current.getId());
        if (!ok) {
            return R.error().message("视频不存在");
        }
        return R.ok();
    }

    /**
     * 获取浏览历史（按日期分组）
     */
    @GetMapping("/history")
    public R<?> history(@RequestParam(required = false) Long page, @RequestParam(required = false) Long limit) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        LinkedHashMap<String, List<Video>> history = videoService.getHistory(current.getId(), page, limit);
        LinkedHashMap<String, List<VideoVO>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Video>> entry : history.entrySet()) {
            List<VideoVO> voList = videoVoConverter.toVideoVOList(entry.getValue());
            result.put(entry.getKey(), voList);
        }
        return R.ok().data(result);
    }

    /**
     * 初始化关注流（拉模式）
     */
    @PostMapping("/init/follow/feed")
    public R<?> initFollowFeed() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        videoService.initFollowFeed(current.getId());
        return R.ok();
    }

    /**
     * 获取关注流视频列表
     */
    @GetMapping("/follow/feed")
    public R<?> followFeed(@RequestParam(required = false) Long lastTime) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        List<Video> videos = videoService.followFeed(current.getId(), lastTime);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 获取好友分享流视频列表
     */
    @GetMapping("/share/friend/feed")
    public R<?> friendShareFeed(@RequestParam(required = false) Long lastTime) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        List<Video> videos = videoService.friendShareFeed(current.getId(), lastTime);
        List<VideoVO> result = videoVoConverter.toVideoVOList(videos);
        return R.ok().data(result);
    }

    /**
     * 获取审核队列状态
     */
    @GetMapping("/audit/queue/state")
    public R<?> auditQueueState() {
        return R.ok().message(videoService.getAuditQueueState());
    }

    /**
     * 删除当前用户的视频
     */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        boolean ok = videoService.deleteVideo(id, current.getId());
        return ok ? R.ok().message("删除成功") : R.error().message("视频不存在或无删除权限");
    }
}
