package com.example.simpletiktok.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.simpletiktok.pojo.dto.FavoritesSaveDTO;
import com.example.simpletiktok.pojo.entity.Favorites;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.vo.FavoritesVO;
import com.example.simpletiktok.service.IFavoritesService;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class FavoritesController {

    private final IFavoritesService favoritesService;

    /**
     * 获取当前用户的收藏夹列表。
     *
     * 测试地址:
     * GET http://localhost:8080/api/customer/favorites
     *
     * 测试示例:
     * curl "http://localhost:8080/api/customer/favorites"
     */
    @GetMapping("/favorites")
    public R<?> listFavorites() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        List<Favorites> list = favoritesService.listByUserId(current.getId());
        List<FavoritesVO> result = list.stream()
                .map(item -> BeanUtil.copyProperties(item, FavoritesVO.class))
                .collect(Collectors.toList());
        return R.ok().data(result);
    }

    /**
     * 获取指定收藏夹。
     *
     * 测试地址:
     * GET http://localhost:8080/api/customer/favorites/{id}
     *
     * 测试示例:
     * curl "http://localhost:8080/api/customer/favorites/1"
     */
    @GetMapping("/favorites/{id}")
    public R<?> getFavorites(@PathVariable Long id) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        Favorites favorites = favoritesService.getByIdAndUser(id, current.getId());
        if (favorites == null) {
            return R.error().message("收藏夹不存在");
        }
        return R.ok().data(BeanUtil.copyProperties(favorites, FavoritesVO.class));
    }

    /**
     * 新增/修改收藏夹。
     *
     * 测试地址:
     * POST http://localhost:8080/api/customer/favorites
     *
     * 新增测试示例:
     * curl -X POST "http://localhost:8080/api/customer/favorites" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"name\":\"旅行\",\"description\":\"我的旅行收藏\"}"
     *
     * 修改测试示例(带id):
     * curl -X POST "http://localhost:8080/api/customer/favorites" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"id\":1,\"name\":\"旅行\",\"description\":\"更新后的描述\"}"
     */
    @PostMapping("/favorites")
    public R<?> saveOrUpdateFavorites(@RequestBody FavoritesSaveDTO dto) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        if (dto == null || StrUtil.isBlank(dto.getName())) {
            return R.error().message("收藏夹名称不能为空");
        }
        if (favoritesService.existsByName(current.getId(), dto.getName(), dto.getId())) {
            return R.error().message("已存在相同名称的收藏夹");
        }
        if (dto.getId() != null) {
            Favorites existing = favoritesService.getByIdAndUser(dto.getId(), current.getId());
            if (existing == null) {
                return R.error().message("收藏夹不存在");
            }
            if (IFavoritesService.DEFAULT_FAVORITES_NAME.equals(existing.getName())) {
                return R.error().message("默认收藏夹不允许修改");
            }
        }
        Favorites favorites = BeanUtil.copyProperties(dto, Favorites.class);
        favorites.setUserId(current.getId());
        boolean ok = favoritesService.saveOrUpdate(favorites);
        return ok ? R.ok() : R.error().message("保存失败");
    }

    /**
     * 删除收藏夹。
     *
     * 测试地址:
     * DELETE http://localhost:8080/api/customer/favorites/{id}
     *
     * 测试示例:
     * curl -X DELETE "http://localhost:8080/api/customer/favorites/1"
     */
    @DeleteMapping("/favorites/{id}")
    public R<?> deleteFavorites(@PathVariable Long id) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        Favorites favorites = favoritesService.getByIdAndUser(id, current.getId());
        if (favorites == null) {
            return R.error().message("收藏夹不存在");
        }
        if (IFavoritesService.DEFAULT_FAVORITES_NAME.equals(favorites.getName())) {
            return R.error().message("默认收藏夹不允许被删除");
        }
        boolean ok = favoritesService.removeFavorites(id, current.getId());
        return ok ? R.ok() : R.error().message("删除失败");
    }
}
