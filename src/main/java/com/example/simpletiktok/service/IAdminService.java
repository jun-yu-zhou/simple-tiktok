package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.entity.Admin;

public interface IAdminService extends IService<Admin> {

    Admin findByUsername(String username);

    void updateLastLoginTime(Long adminId);
}
