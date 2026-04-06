package com.example.simpletiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.simpletiktok.pojo.dto.UserUpdateDTO;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.vo.CustomerInfoVO;
import com.example.simpletiktok.pojo.vo.CustomerRelationPageVO;

import java.util.Collection;

public interface IUserService extends IService<User> {
    boolean existsByEmail(String email);

    User findByEmail(String email);

    Long createUser(User user);

    CustomerInfoVO getInfo(Long userId);

    CustomerRelationPageVO getFollows(Long userId, Long page, Long limit);

    CustomerRelationPageVO getFans(Long userId, Long page, Long limit);

    Collection<String> searchHistory(Long userId);

    void addSearchHistory(Long userId, String search);

    void deleteSearchHistory(Long userId);

    String updateUserProfile(Long userId, UserUpdateDTO dto);

    boolean updatePasswordByEmail(String email, String newPassword);
}
