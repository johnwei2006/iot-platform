package com.iot.auth.api;

import com.iot.auth.common.ApiResponse;
import com.iot.auth.common.AppException;
import com.iot.auth.common.IpUtil;
import com.iot.auth.sso.SsoSessionService;
import com.iot.auth.token.RefreshCookieService;
import com.iot.auth.token.TokenClaims;
import com.iot.auth.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/** 认证 REST API。登录/刷新/登出/校验是 SSO、微信、内部服务调用的公共基础。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final RefreshCookieService cookieService;
    private final SsoSessionService ssoSessionService;

    @Autowired
    public AuthController(AuthService authService, TokenService tokenService,
                          RefreshCookieService cookieService, SsoSessionService ssoSessionService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.cookieService = cookieService;
        this.ssoSessionService = ssoSessionService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                     HttpServletRequest httpRequest,
                                                     HttpServletResponse response) {
        AuthDtos.TokenResponse data = authService.login(request, IpUtil.clientIp(httpRequest));
        cookieService.addRefreshCookie(response, data.getRefreshToken());
        // 统一登录页场景：登录成功即建立 SSO 中央会话，后续跨客户端换票免密
        if (data.getUser() != null) {
            ssoSessionService.createSession(data.getUser().getId(), response);
        }
        return ApiResponse.success(data);
    }

    /** 刷新：body 可为空，浏览器端从 HttpOnly Cookie 取 refresh token（轮换后重写 Cookie）。 */
    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.TokenResponse> refresh(
            @RequestBody(required = false) AuthDtos.RefreshRequest request,
            HttpServletRequest httpRequest, HttpServletResponse response) {
        String cookieRefresh = cookieService.extractRefreshCookie(httpRequest);
        AuthDtos.TokenResponse data = authService.refresh(request, cookieRefresh);
        cookieService.addRefreshCookie(response, data.getRefreshToken());
        return ApiResponse.success(data);
    }

    /** 登出：撤销 body/Cookie 中的 refresh、拉黑 access，并清除 Cookie。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) AuthDtos.LogoutRequest request,
                                    HttpServletRequest httpRequest, HttpServletResponse response) {
        if (request == null) {
            request = new AuthDtos.LogoutRequest();
        }
        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            request.setRefreshToken(cookieService.extractRefreshCookie(httpRequest));
        }
        if (request.getRefreshToken() != null || request.getAccessToken() != null) {
            authService.logout(request, IpUtil.clientIp(httpRequest));
        }
        cookieService.clearRefreshCookie(response);
        return ApiResponse.success(null);
    }

    /**
     * 撤销某用户全部会话（递增 tokenVersion）：改密/禁用/管理员踢人时调用。
     * 默认撤销当前 token 所属用户；带 userId 时需要 ADMIN 角色。
     */
    @PostMapping("/revoke-all")
    public ApiResponse<Void> revokeAll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) RevokeAllRequest request,
            HttpServletRequest httpRequest) {
        TokenClaims claims = tokenService.verifyAccessToken(extractBearer(authorization));
        String target = claims.getSubject();
        if (request != null && request.getUserId() != null && !request.getUserId().trim().isEmpty()
                && !request.getUserId().equals(target)) {
            if (!claims.getRoles().contains("ADMIN")) {
                throw AppException.forbidden("admin role required");
            }
            target = request.getUserId();
        }
        authService.revokeAllSessions(target, IpUtil.clientIp(httpRequest));
        return ApiResponse.success(null);
    }

    public static class RevokeAllRequest {
        @NotBlank
        private String userId;
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
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
