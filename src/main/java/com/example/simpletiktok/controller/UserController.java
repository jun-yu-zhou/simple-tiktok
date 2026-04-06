package com.example.simpletiktok.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.simpletiktok.pojo.dto.UserLoginDTO;
import com.example.simpletiktok.pojo.dto.UserRegisterDTO;
import com.example.simpletiktok.pojo.dto.UserUpdateDTO;
import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.pojo.vo.LoginVO;
import com.example.simpletiktok.pojo.vo.UserVO;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.util.JwtUtils;
import com.example.simpletiktok.util.R;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 用户注册。
     *
     * 测试地址:
     * POST http://localhost:8080/api/user/register
     *
     * 测试示例:
     * curl -X POST "http://localhost:8080/api/user/register" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"email\":\"test@example.com\",\"password\":\"123456\",\"nickName\":\"test\"}"
     */
    @PostMapping("/register")
    public R<?> register(@RequestBody UserRegisterDTO dto) {
        if (StrUtil.isBlank(dto.getEmail()) || StrUtil.isBlank(dto.getPassword())) {
            return R.error().message("邮箱和密码不能为空");
        }
        if (StrUtil.isBlank(dto.getNickName())) {
            return R.error().message("昵称不能为空");
        }
        if (userService.existsByEmail(dto.getEmail())) {
            return R.error().message("邮箱已注册");
        }
        User user = BeanUtil.copyProperties(dto, User.class);
        Long userId = userService.createUser(user);
        return R.ok().data(userId);
    }

    /**
     * 用户登录。
     *
     * 测试地址:
     * POST http://localhost:8080/api/user/login
     *
     * 测试示例:
     * curl -X POST "http://localhost:8080/api/user/login" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"email\":\"test@example.com\",\"password\":\"123456\"}"
     */
    @PostMapping("/login")
    public R<?> login(@RequestBody UserLoginDTO dto) {
        if (StrUtil.isBlank(dto.getEmail()) || StrUtil.isBlank(dto.getPassword())) {
            return R.error().message("邮箱和密码不能为空");
        }
        User user = userService.findByEmail(dto.getEmail());
        if (user == null || !StrUtil.equals(user.getPassword(), dto.getPassword())) {
            return R.error().message("账号或密码错误");
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        String token = JwtUtils.getJwtToken(user.getId(), user.getNickName());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(userVO);
        return R.ok().data(vo);
    }

    /**
     * 修改当前登录用户资料。
     *
     * 测试地址:
     * PUT http://localhost:8080/api/user/profile
     *jian
     * 测试示例:
     * curl -X PUT "http://localhost:8080/api/user/profile" ^
     *   -H "token: 你的JWT" ^
     *   -H "Content-Type: application/json" ^
     *   -d "{\"nickName\":\"新昵称\",\"description\":\"新的简介\",\"sex\":1,\"avatar\":\"avatar/1.png\",\"defaultFavoritesId\":1}"
     */
    @PutMapping("/profile")
    public R<?> updateProfile(@RequestBody UserUpdateDTO dto) {
        User current = UserHolder.get();
        if (current == null) {
            return R.error().message("未登录");
        }
        String error = userService.updateUserProfile(current.getId(), dto);
        if (error != null) {
            return R.error().message(error);
        }
        return R.ok().message("修改成功");
    }
}
