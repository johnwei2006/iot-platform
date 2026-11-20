package com.iot.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.ApiResponse;
import com.iot.platform.security.AuthServiceClient;
import com.iot.platform.security.AuthServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 兼容代理：保留 {@code /api/auth/**} 入口，把登录/刷新/登出透明转发到 auth-service。
 * 令牌签发、密码校验、refresh 与黑名单全部由认证服务负责，本模块不再读取用户密码或持有 JWT 密钥。
 * 新客户端应直接调用 auth-service，本控制器后续版本将移除。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AuthController(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody(required = false) String body) {
        return forward("/api/auth/login", body);
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> refresh(@RequestBody(required = false) String body) {
        return forward("/api/auth/refresh", body);
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> logout(@RequestBody(required = false) String body) {
        return forward("/api/auth/logout", body);
    }

    private ResponseEntity<String> forward(String path, String body) {
        try {
            AuthServiceClient.ProxyResponse response =
                    authServiceClient.post(path, body == null ? "{}" : body);
            return ResponseEntity.status(response.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody() == null ? "" : response.getBody());
        } catch (AuthServiceException e) {
            return ResponseEntity.status(e.getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody(e.getStatus(), e.getMessage()));
        }
    }

    private String errorBody(int status, String message) {
        try {
            return objectMapper.writeValueAsString(ApiResponse.fail(status, message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize auth error response failed", e);
        }
    }
}
