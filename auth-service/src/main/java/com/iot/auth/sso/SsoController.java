package com.iot.auth.sso;

import com.iot.auth.api.AuthDtos;
import com.iot.auth.api.AuthService;
import com.iot.auth.common.ApiResponse;
import com.iot.auth.common.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/** SSO 票据接口：登录成功后由 auth-service 签发 ticket，客户端凭 ticket 换令牌。 */
@RestController
@RequestMapping("/api/sso")
public class SsoController {

    private final SsoService ssoService;
    private final AuthService authService;

    @Autowired
    public SsoController(SsoService ssoService, AuthService authService) {
        this.ssoService = ssoService;
        this.authService = authService;
    }

    /** 已登录会话下调用：为指定客户端签发 ticket（由统一登录页在身份确认后触发）。 */
    @PostMapping("/ticket")
    public ApiResponse<TicketResponse> issueTicket(@Valid @RequestBody TicketRequest request) {
        String userId = authService.verify(request.getAccessToken()).getSubject();
        String ticket = ssoService.issueTicket(request.getClientId(),
                request.getRedirectUri(), userId);
        TicketResponse response = new TicketResponse();
        response.setTicket(ticket);
        response.setRedirectUri(request.getRedirectUri());
        return ApiResponse.success(response);
    }

    /** 客户端用一次性 ticket 换取令牌。 */
    @PostMapping("/token")
    public ApiResponse<AuthDtos.TokenResponse> exchange(@Valid @RequestBody ExchangeRequest request) {
        String userId = ssoService.consumeTicket(request.getTicket(), request.getClientId());
        return ApiResponse.success(authService.issueTokensById(userId, request.getClientId(),
                request.getDeviceId()));
    }

    public static class TicketRequest {
        @NotBlank private String accessToken;
        @NotBlank private String clientId;
        @NotBlank private String redirectUri;
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class ExchangeRequest {
        @NotBlank private String ticket;
        @NotBlank private String clientId;
        private String deviceId;
        public String getTicket() { return ticket; }
        public void setTicket(String ticket) { this.ticket = ticket; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
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
