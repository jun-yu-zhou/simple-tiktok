package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.simpletiktok.mapper.FollowMapper;
import com.example.simpletiktok.mapper.UserMapper;
import com.example.simpletiktok.pojo.dto.UserUpdateDTO;
import com.example.simpletiktok.pojo.entity.Follow;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.entity.Favorites;
import com.example.simpletiktok.pojo.vo.CustomerInfoVO;
import com.example.simpletiktok.pojo.vo.CustomerRelationPageVO;
import com.example.simpletiktok.pojo.vo.CustomerRelationVO;
import com.example.simpletiktok.service.IFavoritesService;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.util.RedisConstants;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final IFavoritesService favoritesService;
    private final FollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean existsByEmail(String email) {
        return this.exists(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
    }

    @Override
    public User findByEmail(String email) {
        return this.getOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
    }

    @Override
    @Transactional
    public Long createUser(User user) {
        this.save(user);
        Favorites favorites = new Favorites();
        favorites.setUserId(user.getId());
        favorites.setName(IFavoritesService.DEFAULT_FAVORITES_NAME);
        favoritesService.save(favorites);
        user.setDefaultFavoritesId(favorites.getId());
        this.updateById(user);
        return user.getId();
    }

    @Override
    public CustomerInfoVO getInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = this.getById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            return null;
        }
        CustomerInfoVO vo = BeanUtil.copyProperties(user, CustomerInfoVO.class);
        Long followCount = followMapper.selectCount(
                Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, userId)
        );
        Long fansCount = followMapper.selectCount(
                Wrappers.<Follow>lambdaQuery().eq(Follow::getFollowId, userId)
        );
        vo.setFollowCount(followCount == null ? 0L : followCount);
        vo.setFansCount(fansCount == null ? 0L : fansCount);
        return vo;
    }

    @Override
    public CustomerRelationPageVO getFollows(Long userId, Long page, Long limit) {
        return buildRelationPage(userId, page, limit, true);
    }

    @Override
    public CustomerRelationPageVO getFans(Long userId, Long page, Long limit) {
        return buildRelationPage(userId, page, limit, false);
    }

    private CustomerRelationPageVO buildRelationPage(Long userId, Long page, Long limit, boolean queryFollows) {
        CustomerRelationPageVO result = new CustomerRelationPageVO();
        long pageNo = page == null || page < 1 ? 1L : page;
        long pageSize = limit == null || limit < 1 ? 15L : limit;
        result.setPage(pageNo);
        result.setLimit(pageSize);

        if (userId == null) {
            result.setTotal(0L);
            result.setRecords(Collections.emptyList());
            return result;
        }

        Long total = queryFollows
                ? followMapper.selectCount(Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, userId))
                : followMapper.selectCount(Wrappers.<Follow>lambdaQuery().eq(Follow::getFollowId, userId));
        result.setTotal(total == null ? 0L : total);
        if (result.getTotal() <= 0) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        long offset = (pageNo - 1) * pageSize;
        List<Long> relationIds = queryFollows
                ? followMapper.selectList(
                Wrappers.<Follow>lambdaQuery()
                        .select(Follow::getFollowId)
                        .eq(Follow::getUserId, userId)
                        .orderByDesc(Follow::getGmtCreated)
                        .last("limit " + offset + "," + pageSize)
        ).stream().map(Follow::getFollowId).collect(Collectors.toList())
                : followMapper.selectList(
                Wrappers.<Follow>lambdaQuery()
                        .select(Follow::getUserId)
                        .eq(Follow::getFollowId, userId)
                        .orderByDesc(Follow::getGmtCreated)
                        .last("limit " + offset + "," + pageSize)
        ).stream().map(Follow::getUserId).collect(Collectors.toList());

        if (relationIds.isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        Set<Long> eachSet = queryFollows
                ? followMapper.selectList(
                Wrappers.<Follow>lambdaQuery()
                        .select(Follow::getUserId)
                        .eq(Follow::getFollowId, userId)
        ).stream().map(Follow::getUserId).collect(Collectors.toSet())
                : followMapper.selectList(
                Wrappers.<Follow>lambdaQuery()
                        .select(Follow::getFollowId)
                        .eq(Follow::getUserId, userId)
        ).stream().map(Follow::getFollowId).collect(Collectors.toSet());

        Map<Long, User> userMap = listUserMapByIds(relationIds);
        List<CustomerRelationVO> records = new ArrayList<>();
        for (Long relationId : relationIds) {
            User user = userMap.get(relationId);
            if (user == null) {
                continue;
            }
            CustomerRelationVO vo = BeanUtil.copyProperties(user, CustomerRelationVO.class);
            vo.setEach(eachSet.contains(relationId));
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    private Map<Long, User> listUserMapByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> idSet = new HashSet<>(userIds);
        return this.list(
                        Wrappers.<User>lambdaQuery()
                                .in(User::getId, idSet)
                                .and(wrapper -> wrapper.isNull(User::getIsDeleted).or().eq(User::getIsDeleted, 0))
                )
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    @Override
    public Collection<String> searchHistory(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String key = RedisConstants.USER_SEARCH_HISTORY + userId;
        Set<String> values = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 19);
        if (values == null) {
            return Collections.emptyList();
        }
        return new LinkedHashSet<>(values);
    }

    @Override
    public void addSearchHistory(Long userId, String search) {
        if (userId == null || search == null || search.isBlank()) {
            return;
        }
        String key = RedisConstants.USER_SEARCH_HISTORY + userId;
        stringRedisTemplate.opsForZSet().add(key, search, System.currentTimeMillis());
        stringRedisTemplate.expire(key, Duration.ofSeconds(RedisConstants.USER_SEARCH_HISTORY_TTL));
    }

    @Override
    public void deleteSearchHistory(Long userId) {
        if (userId == null) {
            return;
        }
        String key = RedisConstants.USER_SEARCH_HISTORY + userId;
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public String updateUserProfile(Long userId, UserUpdateDTO dto) {
        if (userId == null) {
            return "未登录";
        }
        if (dto == null) {
            return "请求参数不能为空";
        }

        User user = this.getById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            return "用户不存在";
        }

        User updateUser = new User();
        updateUser.setId(userId);
        boolean hasUpdateField = false;

        if (dto.getNickName() != null) {
            if (StrUtil.isBlank(dto.getNickName())) {
                return "昵称不能为空";
            }
            updateUser.setNickName(dto.getNickName().trim());
            hasUpdateField = true;
        }

        if (dto.getDescription() != null) {
            updateUser.setDescription(dto.getDescription().trim());
            hasUpdateField = true;
        }

        if (dto.getSex() != null) {
            if (dto.getSex() != 0 && dto.getSex() != 1) {
                return "性别参数不合法";
            }
            updateUser.setSex(dto.getSex());
            hasUpdateField = true;
        }

        if (dto.getAvatar() != null) {
            if (StrUtil.isBlank(dto.getAvatar())) {
                return "头像不能为空";
            }
            updateUser.setAvatar(dto.getAvatar().trim());
            hasUpdateField = true;
        }

        if (dto.getDefaultFavoritesId() != null) {
            if (!favoritesService.existsById(dto.getDefaultFavoritesId(), userId)) {
                return "默认收藏夹不存在";
            }
            updateUser.setDefaultFavoritesId(dto.getDefaultFavoritesId());
            hasUpdateField = true;
        }

        if (!hasUpdateField) {
            return "没有可更新字段";
        }

        boolean ok = this.updateById(updateUser);
        return ok ? null : "修改失败";
    }

    @Override
    public boolean updatePasswordByEmail(String email, String newPassword) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(newPassword)) {
            return false;
        }
        return this.update(
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getEmail, email)
                        .and(wrapper -> wrapper.isNull(User::getIsDeleted).or().eq(User::getIsDeleted, 0))
                        .set(User::getPassword, newPassword)
        );
    }
}
