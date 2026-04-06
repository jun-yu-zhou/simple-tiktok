package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.Type;

import java.util.List;

public interface ITypeService extends IService<Type> {
    List<Type> listOpen();

    Type getOpenById(Long id);

    List<Long> listUserSubscribeTypeIds(Long userId);

    List<Type> listUserSubscribeTypes(Long userId);

    List<Type> listUserNoSubscribeTypes(Long userId);

    boolean subscribeType(Long userId, Long typeId);

    boolean unsubscribeType(Long userId, Long typeId);

    boolean replaceSubscribeTypes(Long userId, List<Long> typeIds);
}
