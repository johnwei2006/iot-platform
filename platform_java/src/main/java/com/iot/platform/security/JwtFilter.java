package com.iot.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 统一鉴权过滤器：校验 Bearer 令牌，白名单路径跳过。
 *
 * <p>验签委托给 auth-service（{@link AuthServiceClient}），本地不再持有 JWT 密钥，
 * 从而由认证服务统一执行签名校验、过期检查、黑名单与 tokenVersion 校验。
 * 校验通过后把身份声明写入 request 属性：subject、clientId、roles、tokenVersion。
 * 白名单：登录、健康检查、actuator、静态资源。
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> whiteList = Arrays.asList(
            "/api/auth/**", "/health", "/actuator/**", "/error", "/favicon.ico");

    public JwtFilter(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String p : whiteList) {
            if (matcher.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "missing Authorization header");
            return;
        }
        try {
            AuthServiceClient.TokenVerification verification =
                    authServiceClient.verify(header.substring("Bearer ".length()).trim());
            request.setAttribute("subject", verification.getSubject());
            request.setAttribute("clientId", verification.getClientId());
            request.setAttribute("roles", verification.getRoles());
            request.setAttribute("tokenVersion", verification.getTokenVersion());
        } catch (AuthServiceException e) {
            writeError(response, e.getStatus(), e.getMessage());
            return;
        } catch (Exception e) {
            logger.warn("Unexpected failure verifying access token", e);
            writeError(response, 503, "auth service unavailable");
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> r = ApiResponse.fail(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
