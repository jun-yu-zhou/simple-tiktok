package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.FavoritesVideoMapper;
import com.example.simpletiktok.pojo.entity.FavoritesVideo;
import com.example.simpletiktok.service.IFavoritesVideoService;
import org.springframework.stereotype.Service;

@Service
public class FavoritesVideoServiceImpl extends ServiceImpl<FavoritesVideoMapper, FavoritesVideo> implements IFavoritesVideoService {
}
