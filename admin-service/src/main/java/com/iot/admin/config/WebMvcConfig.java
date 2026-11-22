package com.iot.admin.config;

import com.iot.admin.security.AdminRoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC 配置：注册管理端 RBAC 拦截器（认证由 JwtAuthFilter 完成）。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminRoleInterceptor roleInterceptor = new AdminRoleInterceptor();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor).addPathPatterns("/api/**");
    }
}
