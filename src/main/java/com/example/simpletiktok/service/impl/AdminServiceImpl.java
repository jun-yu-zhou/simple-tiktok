package com.example.simpletiktok.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.simpletiktok.mapper.AdminMapper;
import com.example.simpletiktok.pojo.entity.Admin;
import com.example.simpletiktok.service.IAdminService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    @Override
    public Admin findByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return this.getOne(
                Wrappers.<Admin>lambdaQuery()
                        .eq(Admin::getUsername, username.trim())
        );
    }

    @Override
    public void updateLastLoginTime(Long adminId) {
        if (adminId == null || adminId <= 0) {
            return;
        }
        this.update(
                Wrappers.<Admin>lambdaUpdate()
                        .eq(Admin::getId, adminId)
                        .set(Admin::getLastLoginTime, LocalDateTime.now())
        );
    }
}
