package com.iot.auth.sso;

import com.iot.api.auth.AuthDtos;
import com.iot.auth.api.AuthService;
import com.iot.auth.audit.AuditService;
import com.iot.api.auth.ApiResponse;
import com.iot.api.auth.AppException;
import com.iot.auth.common.IpUtil;
import com.iot.auth.token.RefreshCookieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** SSO 票据接口：登录成功后由 auth-service 签发 ticket，客户端凭 ticket 换令牌。 */
@RestController
@RequestMapping("/api/sso")
public class SsoController {

    private final SsoService ssoService;
    private final AuthService authService;
    private final SsoSessionService sessionService;
    private final RefreshCookieService cookieService;
    private final AuditService auditService;

    @Autowired
    public SsoController(SsoService ssoService, AuthService authService,
                         SsoSessionService sessionService, RefreshCookieService cookieService,
                         AuditService auditService) {
        this.ssoService = ssoService;
        this.authService = authService;
        this.sessionService = sessionService;
        this.cookieService = cookieService;
        this.auditService = auditService;
    }

    /**
     * 为指定客户端签发一次性 ticket。身份来源二选一：
     * Authorization Bearer access token，或统一登录页建立的中央会话 Cookie。
     */
    @PostMapping("/ticket")
    public ApiResponse<TicketResponse> issueTicket(@Valid @RequestBody TicketRequest request,
                                                   HttpServletRequest httpRequest) {
        String userId = resolveUserId(request.getAccessToken(), httpRequest);
        String ticket = ssoService.issueTicket(request.getClientId(),
                request.getRedirectUri(), userId);
        TicketResponse response = new TicketResponse();
        response.setTicket(ticket);
        response.setRedirectUri(request.getRedirectUri());
        return ApiResponse.success(response);
    }

    /** 统一登录页探测中央会话：已登录返回 userId，未登录 authenticated=false。 */
    @GetMapping("/session")
    public ApiResponse<SessionResponse> session(HttpServletRequest request) {
        SessionResponse response = new SessionResponse();
        String userId = sessionService.resolveUserId(request);
        response.setAuthenticated(userId != null);
        response.setUserId(userId);
        return ApiResponse.success(response);
    }

    /** 单点登出：销毁中央会话 + 递增 tokenVersion 撤销该用户全部令牌 + 清 refresh Cookie。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String userId = sessionService.destroySession(request, response);
        if (userId != null) {
            authService.revokeAllSessions(userId, IpUtil.clientIp(request));
        }
        cookieService.clearRefreshCookie(response);
        return ApiResponse.success(null);
    }

    private String resolveUserId(String accessToken, HttpServletRequest request) {
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return authService.verify(accessToken).getSubject();
        }
        String userId = sessionService.resolveUserId(request);
        if (userId == null) {
            throw AppException.unauthorized("no active sso session or access token");
        }
        return userId;
    }

    /** 客户端用一次性 ticket + clientSecret + redirectUri 换取令牌（轮换 refresh 走 Cookie）。 */
    @PostMapping("/token")
    public ApiResponse<AuthDtos.TokenResponse> exchange(@Valid @RequestBody ExchangeRequest request,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse response) {
        String userId = ssoService.consumeTicket(request.getTicket(), request.getClientId(),
                request.getClientSecret(), request.getRedirectUri());
        AuthDtos.TokenResponse data = authService.issueTokensById(userId, request.getClientId(),
                request.getDeviceId());
        cookieService.addRefreshCookie(response, data.getRefreshToken());
        auditService.record(AuditService.SSO_EXCHANGE, userId, null,
                request.getClientId(), IpUtil.clientIp(httpRequest), null);
        return ApiResponse.success(data);
    }

    public static class TicketRequest {
        /** 可选：带 access token 时优先使用；否则走中央会话 Cookie */
        private String accessToken;
        @NotBlank private String clientId;
        @NotBlank private String redirectUri;
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class SessionResponse {
        private boolean authenticated;
        private String userId;
        public boolean isAuthenticated() { return authenticated; }
        public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class ExchangeRequest {
        @NotBlank private String ticket;
        @NotBlank private String clientId;
        /** 客户端已配置密钥时必填（常量时间比对） */
        private String clientSecret;
        /** 必须与签票时绑定的 redirectUri 完全一致 */
        @NotBlank private String redirectUri;
        private String deviceId;
        public String getTicket() { return ticket; }
        public void setTicket(String ticket) { this.ticket = ticket; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    public static class TicketResponse {
        private String ticket;
        private String redirectUri;
        public String getTicket() { return ticket; }
        public void setTicket(String ticket) { this.ticket = ticket; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }
}
