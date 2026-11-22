package com.iot.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.admin.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * RBAC 拦截器：处理方法/类上标注 {@link RequireRoles} 时校验 JwtAuthFilter
 * 写入的角色（request 属性 roles）；SUPER_ADMIN 恒通过。
 */
public class AdminRoleInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminRoleInterceptor.class);
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String SUPER_ADMIN_ATTRIBUTE = "superAdmin";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRoles required = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
        }
        if (required == null) {
            return true;
        }
        Object superRaw = request.getAttribute(SUPER_ADMIN_ATTRIBUTE);
        if (Boolean.TRUE.equals(superRaw)) {
            return true;
        }
        Object raw = request.getAttribute(ROLES_ATTRIBUTE);
        List<?> roles = raw instanceof List ? (List<?>) raw : null;
        if (roles != null) {
            for (String need : required.value()) {
                for (Object role : roles) {
                    if (need.equalsIgnoreCase(String.valueOf(role))) {
                        return true;
                    }
                }
            }
        }
        logger.warn("Admin access denied for {} {}: required {} but has {}",
                request.getMethod(), request.getRequestURI(),
                Arrays.toString(required.value()), roles);
        writeForbidden(response);
        return false;
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(403, "insufficient role")));
    }
}
