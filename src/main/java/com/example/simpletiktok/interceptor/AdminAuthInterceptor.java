package com.example.simpletiktok.interceptor;

import com.example.simpletiktok.pojo.entity.Admin;
import com.example.simpletiktok.service.IAdminService;
import com.example.simpletiktok.util.AdminHolder;
import com.example.simpletiktok.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端鉴权拦截器。
 * 仅校验 JWT，不在服务端保存令牌状态。
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final IAdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);
        if (!JwtUtils.checkAdminToken(token)) {
            writeUnauthorized(response);
            return false;
        }
        Long adminId = JwtUtils.getAdminId(token);
        if (adminId == null || adminId <= 0) {
            writeUnauthorized(response);
            return false;
        }
        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            writeUnauthorized(response);
            return false;
        }
        AdminHolder.set(admin);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminHolder.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("X-Token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        token = request.getHeader("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"state\":false,\"message\":\"unauthorized\"}");
    }
}
