package com.iot.auth.api;

import com.iot.auth.common.ApiResponse;
import com.iot.auth.token.TokenClaims;
import com.iot.auth.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 认证 REST API。登录/刷新/登出/校验是 SSO、微信、内部服务调用的公共基础。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @Autowired
    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) AuthDtos.LogoutRequest request) {
        if (request != null) {
            authService.logout(request);
        }
        return ApiResponse.success(null);
    }

    /** 供内部服务或网关调用：校验 access token 并返回身份声明。 */
    @PostMapping("/verify")
    public ApiResponse<AuthDtos.VerifyResponse> verify(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearer(authorization);
        return ApiResponse.success(authService.verify(token));
    }

    /** 获取身份声明（本地验签，便于调试与内部使用）。 */
    @GetMapping("/claims")
    public ApiResponse<AuthDtos.VerifyResponse> claims(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearer(authorization);
        TokenClaims c = tokenService.verifyAccessToken(token);
        AuthDtos.VerifyResponse response = new AuthDtos.VerifyResponse();
        response.setSubject(c.getSubject());
        response.setClientId(c.getClientId());
        response.setRoles(c.getRoles());
        response.setTokenVersion(c.getTokenVersion());
        return ApiResponse.success(response);
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
