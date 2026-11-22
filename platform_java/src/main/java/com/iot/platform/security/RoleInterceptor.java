package com.iot.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.ApiResponse;
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
 * RBAC 拦截器：处理方法或其类上标注了 {@link RequireRoles} 时，
 * 校验 JwtFilter 写入的角色（request 属性 "roles"），不满足返回 403。
 * 非 Spring bean，由 WebMvcConfig 实例化（避免 @WebMvcTest 切片缺 bean）。
 */
public class RoleInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RoleInterceptor.class);
    private static final String ROLES_ATTRIBUTE = "roles";

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
        logger.warn("Access denied for {} {}: required roles {} but has {}",
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
