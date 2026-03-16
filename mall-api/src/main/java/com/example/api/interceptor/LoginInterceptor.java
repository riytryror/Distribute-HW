package com.example.api.interceptor;

import com.example.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;


// 拦截器实现
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Value("${server.port}")
    private String port; // 注入端口
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只要请求进来，不管拦截与否，先打印端口
        System.out.println(">>> [拦截器] 收到请求，当前处理端口: " + port + "，请求路径: " + request.getRequestURI());
        
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

