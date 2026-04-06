package com.example.simpletiktok.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.example.simpletiktok.pojo.dto.AdminLoginDTO;
import com.example.simpletiktok.pojo.entity.Admin;
import com.example.simpletiktok.pojo.vo.AdminLoginVO;
import com.example.simpletiktok.service.IAdminService;
import com.example.simpletiktok.util.AdminHolder;
import com.example.simpletiktok.util.JwtUtils;
import com.example.simpletiktok.util.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口。
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final IAdminService adminService;

    /**
     * 管理员登录。
     */
    @PostMapping("/login")
    public R<?> login(@RequestBody AdminLoginDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getUsername()) || StrUtil.isBlank(dto.getPassword())) {
            return R.error().message("username or password is empty");
        }
        Admin admin = adminService.findByUsername(dto.getUsername());
        if (admin == null || !StrUtil.equals(admin.getPassword(), dto.getPassword())) {
            return R.error().message("username or password is invalid");
        }
        String token = JwtUtils.getAdminJwtToken(admin.getId(), admin.getUsername());
        adminService.updateLastLoginTime(admin.getId());

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        return R.ok().data(vo);
    }

    /**
     * 获取当前登录管理员信息。
     */
    @GetMapping("/me")
    public R<?> me() {
        Admin admin = AdminHolder.get();
        if (admin == null) {
            return R.error().code(401).message("unauthorized");
        }
        AdminLoginVO vo = new AdminLoginVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        return R.ok().data(vo);
    }

    /**
     * 管理员退出登录。
     * JWT 无状态模式下由前端删除令牌即可。
     */
    @PostMapping("/logout")
    public R<?> logout() {
        return R.ok().message("logout success");
    }
}
