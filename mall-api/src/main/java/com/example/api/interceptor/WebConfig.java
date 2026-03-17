package com.example.api.config;

import com.example.api.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") 
                // 放行：注册、登录、商品列表、商品详情
                .excludePathPatterns(
                    "/api/v1/users/login", 
                    "/api/v1/users/register",
                    "/api/v1/products/**" 
                );
    }
}
