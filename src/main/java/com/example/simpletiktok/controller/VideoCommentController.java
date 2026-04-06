package com.example.simpletiktok.controller;

import com.example.simpletiktok.pojo.dto.VideoCommentSaveDTO;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.vo.VideoCommentVO;
import com.example.simpletiktok.service.IVideoCommentService;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video/comment")
@RequiredArgsConstructor
public class VideoCommentController {

    private final IVideoCommentService videoCommentService;

    /**
     * 发布评论。
     * 规则：发布前先做评论内容审核，通过返回 true，不通过返回 false。
     */
    @PostMapping("/publish")
    public R<?> publish(@RequestBody VideoCommentSaveDTO dto) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        // 发布主评论时强制置为根层，避免前端误传导致语义混乱
        dto.setRootId(0L);
        dto.setParentId(0L);
        boolean ok = videoCommentService.publishComment(dto, current.getId());
        return R.ok().data(ok);
    }

    /**
     * 回复评论。
     * 要求 rootId > 0（指向主评论），parentId 可空（默认回复主评论）。
     */
    @PostMapping("/reply")
    public R<?> reply(@RequestBody VideoCommentSaveDTO dto) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        if (dto == null || dto.getRootId() == null || dto.getRootId() <= 0) {
            return R.ok().data(false);
        }
        if (dto.getParentId() == null || dto.getParentId() <= 0) {
            dto.setParentId(dto.getRootId());
        }
        boolean ok = videoCommentService.publishComment(dto, current.getId());
        return R.ok().data(ok);
    }

    /**
     * 查询评论列表。
     * rootId=0 查主评论，rootId>0 查该主评论下的回复。
     */
    @GetMapping("/list")
    public R<?> list(@RequestParam Long videoId,
                     @RequestParam(required = false) Long rootId,
                     @RequestParam(required = false) Long page,
                     @RequestParam(required = false) Long limit) {
        List<VideoCommentVO> list = videoCommentService.listComments(videoId, rootId, page, limit);
        return R.ok().data(list);
    }

    /**
     * 查询视频评论总数（主评论 + 回复）。
     */
    @GetMapping("/count")
    public R<?> count(@RequestParam Long videoId) {
        long count = videoCommentService.countComments(videoId);
        return R.ok().data(count);
    }
}
