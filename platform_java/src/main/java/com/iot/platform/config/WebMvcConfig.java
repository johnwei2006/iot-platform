package com.iot.platform.config;

import com.iot.platform.security.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册 RBAC 拦截器。
 * 认证（JwtFilter）在前，授权（RoleInterceptor）在后；
 * /api/auth/** 由 JwtFilter 白名单放行，无需角色。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor = new RoleInterceptor();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
