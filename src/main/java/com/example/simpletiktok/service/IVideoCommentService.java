package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.dto.VideoCommentSaveDTO;
import com.example.simpletiktok.pojo.entity.VideoComment;
import com.example.simpletiktok.pojo.vo.VideoCommentVO;

import java.util.List;

public interface IVideoCommentService extends IService<VideoComment> {

    /**
     * 发布评论（含评论内容审核）
     *
     * @return true=通过审核并入库成功；false=审核不通过或参数非法/写库失败
     */
    boolean publishComment(VideoCommentSaveDTO dto, Long userId);

    /**
     * 查询评论列表
     * rootId=0 查询主评论；rootId>0 查询该主评论下的回复
     */
    List<VideoCommentVO> listComments(Long videoId, Long rootId, Long page, Long limit);

    /**
     * 统计视频有效评论总数（包含主评论与回复）
     */
    long countComments(Long videoId);
}
