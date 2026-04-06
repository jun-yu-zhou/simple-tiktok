package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.Favorites;
import com.example.simpletiktok.pojo.vo.FavoriteSelectStateVO;

import java.util.List;

public interface IFavoritesService extends IService<Favorites> {
    String DEFAULT_FAVORITES_NAME = "默认收藏夹";

    List<Favorites> listByUserId(Long userId);

    Favorites getByIdAndUser(Long id, Long userId);

    boolean existsByName(Long userId, String name, Long excludeId);

    boolean removeFavorites(Long id, Long userId);

    List<Long> listVideoIds(Long favoritesId, Long userId);

    boolean toggleFavorite(Long favoritesId, Long videoId, Long userId);

    boolean existsById(Long favoritesId, Long userId);

    /**
     * 查询当前用户各收藏夹对指定视频的收藏状态。
     */
    List<FavoriteSelectStateVO> listFavoriteStates(Long userId, Long videoId);
}
