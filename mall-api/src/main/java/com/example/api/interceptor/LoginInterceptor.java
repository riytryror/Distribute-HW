package com.example.api.interceptor;

import com.example.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;



// 拦截器实现
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("请先登录");
        }
        try {
            Claims claims = JwtUtil.parseToken(token.substring(7));
            request.setAttribute("userId", claims.getSubject());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("登录失效，请重新登录");
        }
    }
}

