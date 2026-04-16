package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.exception.BizException;
import com.example.simpletiktok.mapper.VideoCommentMapper;
import com.example.simpletiktok.pojo.dto.VideoCommentSaveDTO;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.entity.VideoComment;
import com.example.simpletiktok.pojo.vo.VideoCommentVO;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.service.IVideoCommentService;
import com.example.simpletiktok.service.IVideoService;
import com.example.simpletiktok.util.guardrail.CommentGuardrail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoCommentServiceImpl extends ServiceImpl<VideoCommentMapper, VideoComment> implements IVideoCommentService {

    private final IVideoService videoService;
    private final IUserService userService;
    private final CommentGuardrail commentGuardrail;

    @Override
    public boolean publishComment(VideoCommentSaveDTO dto, Long userId) {
        if (dto == null || userId == null || dto.getVideoId() == null || StrUtil.isBlank(dto.getContent())) {
            return false;
        }
        Video video = videoService.getById(dto.getVideoId());
        if (video == null || video.getAuditStatus() == null || video.getAuditStatus() != 1) {
            return false;
        }

        // 评论入库前先做本地敏感词审核，命中违禁词直接返回明确错误信息。
        if (!commentGuardrail.isSafe(dto.getContent())) {
            throw new BizException("言论包含违禁词，发布失败！");
        }

        Long rootId = dto.getRootId() == null ? 0L : dto.getRootId();
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (rootId < 0 || parentId < 0) {
            return false;
        }
        if (rootId == 0 && parentId != 0) {
            return false;
        }
        if (rootId > 0 && parentId == 0) {
            parentId = rootId;
        }

        if (rootId > 0) {
            VideoComment root = this.getById(rootId);
            if (root == null || !dto.getVideoId().equals(root.getVideoId()) || isDeleted(root)) {
                return false;
            }
            if (root.getRootId() != null && root.getRootId() > 0) {
                return false;
            }
        }
        if (parentId > 0) {
            VideoComment parent = this.getById(parentId);
            if (parent == null || !dto.getVideoId().equals(parent.getVideoId()) || isDeleted(parent)) {
                return false;
            }
            if (rootId > 0) {
                // 回复链路校验：父评论必须属于当前根评论
                boolean matchRoot = parent.getId().equals(rootId)
                        || (parent.getRootId() != null && parent.getRootId().equals(rootId));
                if (!matchRoot) {
                    return false;
                }
            }
        }

        VideoComment comment = new VideoComment();
        comment.setVideoId(dto.getVideoId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());
        comment.setRootId(rootId);
        comment.setParentId(parentId);
        comment.setIsDeleted(0);
        return this.save(comment);
    }

    @Override
    public List<VideoCommentVO> listComments(Long videoId, Long rootIdParam, Long pageParam, Long limitParam) {
        if (videoId == null) {
            return Collections.emptyList();
        }
        long page = pageParam == null || pageParam < 1 ? 1L : pageParam;
        long limit = limitParam == null || limitParam < 1 ? 20L : limitParam;
        long offset = (page - 1) * limit;
        long rootId = rootIdParam == null ? 0L : rootIdParam;

        List<VideoComment> comments;
        if (rootId == 0) {
            comments = this.list(
                    Wrappers.<VideoComment>lambdaQuery()
                            .eq(VideoComment::getVideoId, videoId)
                            .eq(VideoComment::getRootId, 0L)
                            .eq(VideoComment::getParentId, 0L)
                            .eq(VideoComment::getIsDeleted, 0)
                            .orderByDesc(VideoComment::getGmtCreated)
                            .last("limit " + offset + "," + limit)
            );
        } else {
            comments = this.list(
                    Wrappers.<VideoComment>lambdaQuery()
                            .eq(VideoComment::getVideoId, videoId)
                            .eq(VideoComment::getRootId, rootId)
                            .ne(VideoComment::getParentId, 0L)
                            .eq(VideoComment::getIsDeleted, 0)
                            .orderByAsc(VideoComment::getGmtCreated)
                            .last("limit " + offset + "," + limit)
            );
        }
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, VideoComment> parentMap = new HashMap<>();
        if (rootId > 0) {
            List<Long> parentIds = comments.stream()
                    .map(VideoComment::getParentId)
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .collect(Collectors.toList());
            if (!parentIds.isEmpty()) {
                parentMap = this.listByIds(parentIds).stream()
                        .collect(Collectors.toMap(VideoComment::getId, c -> c, (a, b) -> a));
            }
        }

        Set<Long> userIds = new LinkedHashSet<>();
        for (VideoComment comment : comments) {
            userIds.add(comment.getUserId());
            if (rootId > 0) {
                VideoComment parent = parentMap.get(comment.getParentId());
                if (parent != null && parent.getUserId() != null) {
                    userIds.add(parent.getUserId());
                }
            }
        }
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<VideoCommentVO> result = new ArrayList<>(comments.size());
        for (VideoComment comment : comments) {
            VideoCommentVO vo = new VideoCommentVO();
            vo.setId(comment.getId());
            vo.setVideoId(comment.getVideoId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setRootId(comment.getRootId());
            vo.setParentId(comment.getParentId());
            vo.setIsDeleted(comment.getIsDeleted());
            vo.setGmtCreated(toEpochMilli(comment.getGmtCreated()));
            vo.setGmtUpdated(toEpochMilli(comment.getGmtUpdated()));

            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setUserNickName(user.getNickName());
                vo.setUserAvatar(user.getAvatar());
            }

            if (rootId == 0) {
                long childCount = this.count(
                        Wrappers.<VideoComment>lambdaQuery()
                                .eq(VideoComment::getVideoId, comment.getVideoId())
                                .eq(VideoComment::getRootId, comment.getId())
                                .ne(VideoComment::getParentId, 0L)
                                .eq(VideoComment::getIsDeleted, 0)
                );
                vo.setChildCount(childCount);
                vo.setHasMoreChildren(childCount > 0);
                vo.setReplyToUserId(null);
                vo.setReplyToNickName(null);
            } else {
                vo.setChildCount(0L);
                vo.setHasMoreChildren(false);
                VideoComment parent = parentMap.get(comment.getParentId());
                if (parent != null) {
                    vo.setReplyToUserId(parent.getUserId());
                    User replyToUser = userMap.get(parent.getUserId());
                    vo.setReplyToNickName(replyToUser == null ? null : replyToUser.getNickName());
                }
            }

            result.add(vo);
        }
        return result;
    }

    @Override
    public long countComments(Long videoId) {
        if (videoId == null) {
            return 0L;
        }
        return this.count(
                Wrappers.<VideoComment>lambdaQuery()
                        .eq(VideoComment::getVideoId, videoId)
                        .eq(VideoComment::getIsDeleted, 0)
        );
    }

    private boolean isDeleted(VideoComment comment) {
        return comment != null && comment.getIsDeleted() != null && comment.getIsDeleted() == 1;
    }

    private Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
