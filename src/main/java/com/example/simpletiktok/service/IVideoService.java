package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.Video;
import com.example.simpletiktok.pojo.vo.HotRankVO;

import java.util.LinkedHashMap;
import java.util.List;

public interface IVideoService extends IService<Video> {
    List<HotRankVO> listHotRank();

    /**
     * 获取热门视频列表：近三天按 10/3/2 规则随机抽样。
     */
    List<Video> listHotVideo();

    List<Video> listByUserIdVideo(Long userId, Long page, Long limit);

    Long countByUserIdVideo(Long userId);

    String getAuditQueueState();

    Integer favoriteVideo(Long favoritesId, Long videoId, Long userId);

    List<Video> listVideoByFavorites(Long favoritesId, Long userId);

    Integer likeVideo(Long videoId, Long userId);

    boolean markUninterested(Long videoId, Long userId);

    boolean addHistory(Long videoId, Long userId);

    LinkedHashMap<String, List<Video>> getHistory(Long userId, Long page, Long limit);

    List<Video> searchByCaption(String search, Long page, Long limit);

    List<Video> listByTypeId(Long typeId, Long page, Long limit);

    List<Video> listByUserIdOpenVideo(Long userId, Long page, Long limit);

    List<Video> listSimilarVideo(Long videoId);

    List<Video> pushVideos(Long userId);

    List<Video> followFeed(Long userId, Long lastTime);

    void initFollowFeed(Long userId);

    int shareVideoToFriend(Long videoId, Long userId, Long friendUserId);

    List<Video> friendShareFeed(Long userId, Long lastTime);

    void pushOutBoxFeed(Long userId, Long videoId, Long time);

    List<Long> listFeedVideoIdsByUserId(Long userId);

    void deleteInBoxFeed(Long userId, List<Long> videoIds);

    void deleteOutBoxFeed(Long userId, List<Long> fans, Long videoId);

    void pushSystemStockIn(Video video);

    void deleteSystemStockIn(Video video);

    void pushSystemTypeStockIn(Video video);

    void deleteSystemTypeStockIn(Video video);

    boolean deleteVideo(Long videoId, Long userId);
}
