package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.FavoritesMapper;
import com.example.simpletiktok.pojo.entity.Favorites;
import com.example.simpletiktok.pojo.entity.FavoritesVideo;
import com.example.simpletiktok.pojo.vo.FavoriteSelectStateVO;
import com.example.simpletiktok.service.IFavoritesService;
import com.example.simpletiktok.service.IFavoritesVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritesServiceImpl extends ServiceImpl<FavoritesMapper, Favorites> implements IFavoritesService {

    private final IFavoritesVideoService favoritesVideoService;

    @Override
    public List<Favorites> listByUserId(Long userId) {
        List<Favorites> favorites = this.list(
                Wrappers.<Favorites>lambdaQuery()
                        .eq(Favorites::getUserId, userId)
                        .eq(Favorites::getIsDeleted, 0)
        );
        if (CollectionUtils.isEmpty(favorites)) {
            return Collections.emptyList();
        }
        List<Long> fIds = favorites.stream().map(Favorites::getId).collect(Collectors.toList());
        Map<Long, Long> fMap = favoritesVideoService.list(
                        Wrappers.<FavoritesVideo>lambdaQuery().in(FavoritesVideo::getFavoritesId, fIds)
                )
                .stream()
                .collect(Collectors.groupingBy(FavoritesVideo::getFavoritesId, Collectors.counting()));
        for (Favorites favorite : favorites) {
            Long videoCount = fMap.get(favorite.getId());
            favorite.setVideoCount(videoCount == null ? 0 : videoCount);
        }
        return favorites;
    }

    @Override
    public Favorites getByIdAndUser(Long id, Long userId) {
        if (id == null || userId == null) {
            return null;
        }
        return this.getOne(
                Wrappers.<Favorites>lambdaQuery()
                        .eq(Favorites::getId, id)
                        .eq(Favorites::getUserId, userId)
                        .eq(Favorites::getIsDeleted, 0)
        );
    }

    @Override
    public boolean existsByName(Long userId, String name, Long excludeId) {
        return this.exists(
                Wrappers.<Favorites>lambdaQuery()
                        .eq(Favorites::getUserId, userId)
                        .eq(Favorites::getName, name)
                        .eq(Favorites::getIsDeleted, 0)
                        .ne(excludeId != null, Favorites::getId, excludeId)
        );
    }

    @Override
    @Transactional
    public boolean removeFavorites(Long id, Long userId) {
        // 删收藏夹
        boolean updated = this.update(
                Wrappers.<Favorites>lambdaUpdate()
                        .eq(Favorites::getId, id)
                        .eq(Favorites::getUserId, userId)
                        .eq(Favorites::getIsDeleted, 0)
                        .set(Favorites::getIsDeleted, 1)
        );
        if (updated) {
            // 删收藏夹里的视频
            favoritesVideoService.remove(
                    Wrappers.<FavoritesVideo>lambdaQuery().eq(FavoritesVideo::getFavoritesId, id)
            );
        }
        return updated;
    }

    @Override
    public List<Long> listVideoIds(Long favoritesId, Long userId) {
        Favorites favorites = getByIdAndUser(favoritesId, userId);
        if (favorites == null) {
            return null;
        }
        return favoritesVideoService.list(
                        Wrappers.<FavoritesVideo>lambdaQuery().eq(FavoritesVideo::getFavoritesId, favoritesId)
                )
                .stream()
                .map(FavoritesVideo::getVideoId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean toggleFavorite(Long favoritesId, Long videoId, Long userId) {
        if (!existsById(favoritesId, userId)) {
            return false;
        }
        try {
            FavoritesVideo favoritesVideo = new FavoritesVideo();
            favoritesVideo.setFavoritesId(favoritesId);
            favoritesVideo.setVideoId(videoId);
            favoritesVideo.setUserId(userId);
            return favoritesVideoService.save(favoritesVideo);
        } catch (Exception e) {
            favoritesVideoService.remove(
                    Wrappers.<FavoritesVideo>lambdaQuery()
                            .eq(FavoritesVideo::getFavoritesId, favoritesId)
                            .eq(FavoritesVideo::getVideoId, videoId)
                            .eq(FavoritesVideo::getUserId, userId)
            );
            return false;
        }
    }

    @Override
    public boolean existsById(Long favoritesId, Long userId) {
        return this.exists(
                Wrappers.<Favorites>lambdaQuery()
                        .eq(Favorites::getId, favoritesId)
                        .eq(Favorites::getUserId, userId)
                        .eq(Favorites::getIsDeleted, 0)
        );
    }

    @Override
    public List<FavoriteSelectStateVO> listFavoriteStates(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return Collections.emptyList();
        }
        List<Favorites> favorites = listByUserId(userId);
        if (CollectionUtils.isEmpty(favorites)) {
            return Collections.emptyList();
        }

        // 一次性查询“该视频在哪些收藏夹里”，避免逐个收藏夹查询（N+1）
        List<Long> favoriteIds = favorites.stream().map(Favorites::getId).collect(Collectors.toList());
        // 查出该视频在哪些收藏夹里
        Set<Long> hasVideoFavoriteIds = favoritesVideoService.list(
                        Wrappers.<FavoritesVideo>lambdaQuery()
                                .in(FavoritesVideo::getFavoritesId, favoriteIds)
                                .eq(FavoritesVideo::getVideoId, videoId)
                                .eq(FavoritesVideo::getUserId, userId)
                )
                .stream()
                .map(FavoritesVideo::getFavoritesId)
                .collect(Collectors.toCollection(HashSet::new));

        // 构建结果
        List<FavoriteSelectStateVO> result = favorites.stream().map(item -> {
            FavoriteSelectStateVO vo = new FavoriteSelectStateVO();
            vo.setId(item.getId());
            vo.setName(item.getName());
            vo.setDescription(item.getDescription());
            vo.setUserId(item.getUserId());
            vo.setVideoCount(item.getVideoCount());
            vo.setHasVideo(hasVideoFavoriteIds.contains(item.getId())); // 当前视频是否已在该收藏夹中
            return vo;
        }).collect(Collectors.toList());
        return result;
    }
}
