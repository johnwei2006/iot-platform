package com.iot.auth.wechat;

import com.iot.auth.api.AuthDtos;
import com.iot.auth.api.AuthService;
import com.iot.auth.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/** 微信小程序登录与手机号绑定端点。 */
@RestController
@RequestMapping("/api/auth/wechat")
public class WechatController {

    private final WechatService wechatService;
    private final AuthService authService;

    @Autowired
    public WechatController(WechatService wechatService, AuthService authService) {
        this.wechatService = wechatService;
        this.authService = authService;
    }

    /** 小程序 wx.login 拿到的 code 换本平台令牌。 */
    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success(wechatService.login(request.getCode(),
                request.getClientId() == null ? "mini" : request.getClientId(),
                request.getDeviceId()));
    }

    /** getPhoneNumber 的 code 绑定手机号到当前登录用户。 */
    @PostMapping("/bindPhone")
    public ApiResponse<Void> bindPhone(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody BindPhoneRequest request) {
        String token = authorization == null || !authorization.startsWith("Bearer ")
                ? null : authorization.substring("Bearer ".length()).trim();
        String userId = authService.verify(token).getSubject();
        wechatService.bindPhone(userId, request.getCode());
        return ApiResponse.success(null);
    }

    public static class WechatLoginRequest {
        @NotBlank private String code;
        private String clientId = "mini";
        private String deviceId;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    public static class BindPhoneRequest {
        @NotBlank private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
