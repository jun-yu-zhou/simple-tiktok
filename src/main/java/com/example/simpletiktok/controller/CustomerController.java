package com.example.simpletiktok.controller;

import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.vo.CustomerInfoVO;
import com.example.simpletiktok.pojo.vo.CustomerRelationPageVO;
import com.example.simpletiktok.service.IFollowService;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final IUserService userService;
    private final IFollowService followService;

    /**
     * 获取指定用户信息
     */
    @GetMapping("/getInfo/{userId}")
    public R<?> getInfoByUserId(@PathVariable Long userId) {
        CustomerInfoVO info = userService.getInfo(userId);
        if (info == null) {
            return R.error().message("用户不存在");
        }
        return R.ok().data(info);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/getInfo")
    public R<?> getDefaultInfo() {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        CustomerInfoVO info = userService.getInfo(current.getId());
        if (info == null) {
            return R.error().message("用户不存在");
        }
        return R.ok().data(info);
    }

    /**
     * 关注/取关
     */
    @PostMapping("/follows")
    public R<?> followSwitch(@RequestParam Long followsUserId) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        if (followsUserId == null) {
            return R.error().message("followsUserId 不能为空");
        }
        if (current.getId().equals(followsUserId)) {
            return R.error().message("不能关注自己");
        }

        // 关注/取关
        boolean following = followService.isFollowing(current.getId(), followsUserId);
        boolean ok = following
                ? followService.unfollow(current.getId(), followsUserId) // 取关
                : followService.follow(current.getId(), followsUserId); // 关注
        if (!ok) {
            return R.error().message("操作失败");
        }
        return R.ok().message(following ? "已取关" : "已关注").data(!following);
    }

    /**
     * 获取关注列表
     * userId 为空时默认查询当前登录用户
     */
    @GetMapping("/follows")
    public R<?> follows(@RequestParam(required = false) Long userId,
                        @RequestParam(required = false) Long page,
                        @RequestParam(required = false) Long limit) {
        Long targetUserId = resolveTargetUserId(userId);
        if (targetUserId == null) {
            return R.error().message("未登录");
        }
        CustomerRelationPageVO data = userService.getFollows(targetUserId, page, limit);
        return R.ok().data(data);
    }

    /**
     * 获取粉丝列表
     * userId 为空时默认查询当前登录用户
     */
    @GetMapping("/fans")
    public R<?> fans(@RequestParam(required = false) Long userId,
                     @RequestParam(required = false) Long page,
                     @RequestParam(required = false) Long limit) {
        Long targetUserId = resolveTargetUserId(userId);
        if (targetUserId == null) {
            return R.error().message("未登录");
        }
        CustomerRelationPageVO data = userService.getFans(targetUserId, page, limit);
        return R.ok().data(data);
    }

    private Long resolveTargetUserId(Long userId) {
        if (userId != null) {
            return userId;
        }
        User current = UserHolder.get();
        return current == null ? null : current.getId();
    }
}

