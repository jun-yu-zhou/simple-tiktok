package com.example.simpletiktok.interceptor;

import com.example.simpletiktok.pojo.entity.User;
import com.example.simpletiktok.service.IUserService;
import com.example.simpletiktok.util.JwtUtils;
import com.example.simpletiktok.util.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("token");
        if (JwtUtils.checkToken(token)) {
            Long userId = JwtUtils.getUserId(token);
            if (userId != null) {
                User user = userService.getById(userId);
                if (user != null) {
                    UserHolder.set(user);
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.clear();
    }
}