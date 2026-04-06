package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mq.TypeLabelSyncProducer;
import com.example.simpletiktok.mapper.TypeMapper;
import com.example.simpletiktok.mapper.UserSubscribeMapper;
import com.example.simpletiktok.pojo.entity.Type;
import com.example.simpletiktok.pojo.entity.UserSubscribe;
import com.example.simpletiktok.service.ITypeService;
import com.example.simpletiktok.service.IUserModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypeServiceImpl extends ServiceImpl<TypeMapper, Type> implements ITypeService {
    private static final double SUBSCRIBE_DELTA = 1D;

    private final UserSubscribeMapper userSubscribeMapper;
    private final IUserModelService userModelService;
    private final TypeLabelSyncProducer typeLabelSyncProducer;

    @Override
    public boolean save(Type entity) {
        boolean ok = super.save(entity);
        if (ok) {
            sendTypeLabelsForSync(entity == null ? null : entity.getLabelNames());
        }
        return ok;
    }

    @Override
    public boolean updateById(Type entity) {
        boolean ok = super.updateById(entity);
        if (ok && entity != null && entity.getLabelNames() != null) {
            sendTypeLabelsForSync(entity.getLabelNames());
        }
        return ok;
    }

    @Override
    public List<Type> listOpen() {
        return this.list(
                Wrappers.<Type>lambdaQuery()
                        .eq(Type::getOpen, 1)
                        .eq(Type::getIsDeleted, 0)
                        .orderByAsc(Type::getSort, Type::getId)
        );
    }

    @Override
    public Type getOpenById(Long id) {
        return this.getOne(
                Wrappers.<Type>lambdaQuery()
                        .eq(Type::getId, id)
                        .eq(Type::getOpen, 1)
                        .eq(Type::getIsDeleted, 0)
        );
    }

    @Override
    public List<Long> listUserSubscribeTypeIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userSubscribeMapper.selectList(
                        Wrappers.<UserSubscribe>lambdaQuery().eq(UserSubscribe::getUserId, userId)
                )
                .stream()
                .map(UserSubscribe::getTypeId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Type> listUserSubscribeTypes(Long userId) {
        List<Long> typeIds = listUserSubscribeTypeIds(userId);
        if (typeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.list(
                Wrappers.<Type>lambdaQuery()
                        .in(Type::getId, typeIds)
                        .eq(Type::getOpen, 1)
                        .eq(Type::getIsDeleted, 0)
                        .orderByAsc(Type::getSort, Type::getId)
        );
    }

    @Override
    public List<Type> listUserNoSubscribeTypes(Long userId) {
        Set<Long> subscribeSet = new LinkedHashSet<>(listUserSubscribeTypeIds(userId));
        return listOpen().stream()
                .filter(type -> !subscribeSet.contains(type.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean subscribeType(Long userId, Long typeId) {
        if (userId == null || typeId == null) {
            return false;
        }
        boolean exists = userSubscribeMapper.exists(
                Wrappers.<UserSubscribe>lambdaQuery()
                        .eq(UserSubscribe::getUserId, userId)
                        .eq(UserSubscribe::getTypeId, typeId)
        );
        if (exists) {
            if (!userModelService.hasModel(userId)) {
                userModelService.initModelByLabels(userId, collectLabelsByTypeIds(listUserSubscribeTypeIds(userId)));
            }
            return true;
        }
        UserSubscribe subscribe = new UserSubscribe();
        subscribe.setUserId(userId);
        subscribe.setTypeId(typeId);
        boolean ok = userSubscribeMapper.insert(subscribe) > 0;
        if (ok) {
            // 新用户模型不存在：用当前已订阅分类做初始化
            // 老用户模型已存在：仅上调本次新增分类中“模型还没有”的标签
            if (!userModelService.hasModel(userId)) {
                userModelService.initModelByLabels(userId, collectLabelsByTypeIds(listUserSubscribeTypeIds(userId)));
            } else {
                userModelService.increaseMissingLabels(
                        userId,
                        collectLabelsByTypeIds(Collections.singletonList(typeId)),
                        SUBSCRIBE_DELTA
                );
            }
        }
        return ok;
    }

    @Override
    public boolean unsubscribeType(Long userId, Long typeId) {
        if (userId == null || typeId == null) {
            return false;
        }
        List<String> removedLabels = collectLabelsByTypeIds(Collections.singletonList(typeId));
        boolean ok = userSubscribeMapper.delete(
                Wrappers.<UserSubscribe>lambdaQuery()
                        .eq(UserSubscribe::getUserId, userId)
                        .eq(UserSubscribe::getTypeId, typeId)
        ) > 0;
        if (ok) {
            // 模型不存在时，按当前已订阅分类初始化（兜底）
            // 模型存在时，仅下调本次取消分类中“模型已有”的标签
            if (!userModelService.hasModel(userId)) {
                userModelService.initModelByLabels(userId, collectLabelsByTypeIds(listUserSubscribeTypeIds(userId)));
            } else {
                userModelService.decreaseExistingLabels(userId, removedLabels, SUBSCRIBE_DELTA);
            }
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean replaceSubscribeTypes(Long userId, List<Long> typeIds) {
        if (userId == null) {
            return false;
        }
        List<Long> oldTypeIds = listUserSubscribeTypeIds(userId);
        List<Long> uniqueTypeIds = typeIds == null
                ? Collections.emptyList()
                : typeIds.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());

        if (!uniqueTypeIds.isEmpty()) {
            long validCount = this.count(
                    Wrappers.<Type>lambdaQuery()
                            .in(Type::getId, uniqueTypeIds)
                            .eq(Type::getOpen, 1)
                            .eq(Type::getIsDeleted, 0)
            );
            if (validCount != uniqueTypeIds.size()) {
                return false;
            }
        }

        userSubscribeMapper.delete(Wrappers.<UserSubscribe>lambdaQuery().eq(UserSubscribe::getUserId, userId));
        for (Long typeId : uniqueTypeIds) {
            UserSubscribe subscribe = new UserSubscribe();
            subscribe.setUserId(userId);
            subscribe.setTypeId(typeId);
            userSubscribeMapper.insert(subscribe);
        }

        if (!userModelService.hasModel(userId)) {
            // 新用户首次引导：直接按当前订阅结果初始化模型
            userModelService.initModelByLabels(userId, collectLabelsByTypeIds(uniqueTypeIds));
            return true;
        }

        // 老用户：只处理订阅差量，保留点赞/收藏/浏览沉淀
        Set<Long> oldSet = new LinkedHashSet<>(oldTypeIds);
        Set<Long> newSet = new LinkedHashSet<>(uniqueTypeIds);
        List<Long> addedTypeIds = newSet.stream().filter(id -> !oldSet.contains(id)).collect(Collectors.toList());
        List<Long> removedTypeIds = oldSet.stream().filter(id -> !newSet.contains(id)).collect(Collectors.toList());

        if (!addedTypeIds.isEmpty()) {
            userModelService.increaseMissingLabels(
                    userId,
                    collectLabelsByTypeIds(addedTypeIds),
                    SUBSCRIBE_DELTA
            );
        }
        if (!removedTypeIds.isEmpty()) {
            userModelService.decreaseExistingLabels(
                    userId,
                    collectLabelsByTypeIds(removedTypeIds),
                    SUBSCRIBE_DELTA
            );
        }
        return true;
    }

    private List<String> collectLabelsByTypeIds(List<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Type> types = this.list(Wrappers.<Type>lambdaQuery().in(Type::getId, typeIds));
        if (types == null || types.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> labels = new ArrayList<>();
        for (Type type : types) {
            if (type.getLabelNames() == null || type.getLabelNames().isEmpty()) {
                continue;
            }
            labels.addAll(type.getLabelNames());
        }
        return labels;
    }

    private void sendTypeLabelsForSync(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return;
        }
        List<String> normalized = labels.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return;
        }
        typeLabelSyncProducer.send(normalized);
    }
}
