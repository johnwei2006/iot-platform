package com.iot.auth.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.auth.common.ApiResponse;
import com.iot.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 内部服务 HMAC 鉴权过滤器（仅作用于 /internal/**）：
 * 校验 X-Service-Id 已注册、X-Timestamp 在 ±5 分钟内、X-Signature 常量时间比对。
 * 失败统一返回 401 JSON；其余路径直接放行。
 */
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthFilter.class);
    private static final List<String> SAFE_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE");

    private final AuthProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public InternalAuthFilter(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String serviceId = request.getHeader(HmacSigner.HEADER_SERVICE_ID);
        String timestamp = request.getHeader(HmacSigner.HEADER_TIMESTAMP);
        String signature = request.getHeader(HmacSigner.HEADER_SIGNATURE);

        if (serviceId == null || timestamp == null || signature == null) {
            writeUnauthorized(response, "missing internal auth headers");
            return;
        }
        String secret = properties.findServiceSecret(serviceId);
        if (secret == null) {
            log.warn("Internal call from unregistered service: {}", serviceId);
            writeUnauthorized(response, "unknown service id");
            return;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            writeUnauthorized(response, "invalid timestamp");
            return;
        }
        if (Math.abs(System.currentTimeMillis() - ts) > HmacSigner.TIMESTAMP_TOLERANCE_MS) {
            writeUnauthorized(response, "timestamp out of range");
            return;
        }
        byte[] body = CachedBodyRequest.readBody(request);
        String expected = HmacSigner.sign(secret, serviceId, timestamp.trim(),
                request.getMethod().toUpperCase(), request.getRequestURI(), body);
        if (!HmacSigner.verify(expected, signature)) {
            log.warn("Internal HMAC signature mismatch: service={} path={}", serviceId, request.getRequestURI());
            writeUnauthorized(response, "invalid signature");
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, message)));
    }
}
