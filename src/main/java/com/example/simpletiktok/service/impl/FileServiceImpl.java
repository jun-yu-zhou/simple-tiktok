package com.example.simpletiktok.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.FileMapper;
import com.example.simpletiktok.pojo.entity.File;
import com.example.simpletiktok.service.IFileService;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements IFileService {
}
