package com.iot.auth.sso;

import com.iot.auth.api.AuthService;
import com.iot.auth.config.AuthProperties;
import com.iot.auth.token.RefreshCookieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SSO 页面入口。
 *
 * <ul>
 *   <li>GET /sso/login：统一登录页（静态页，登录成功建立中央会话并带 ticket 跳回客户端）。</li>
 *   <li>GET /sso/logout：单点登出——销毁中央会话、递增 tokenVersion 撤销该用户全部令牌、
 *       清 refresh Cookie；跳回客户端注册的 postLogoutRedirectUri，否则展示登出页。</li>
 * </ul>
 */
@Controller
public class SsoPageController {

    private final SsoSessionService sessionService;
    private final AuthService authService;
    private final RefreshCookieService cookieService;
    private final AuthProperties properties;

    @Autowired
    public SsoPageController(SsoSessionService sessionService, AuthService authService,
                             RefreshCookieService cookieService, AuthProperties properties) {
        this.sessionService = sessionService;
        this.authService = authService;
        this.cookieService = cookieService;
        this.properties = properties;
    }

    @GetMapping("/sso/login")
    public String loginPage() {
        return "forward:/sso/login.html";
    }

    @GetMapping("/sso/logout")
    public String logout(@RequestParam(value = "postLogoutRedirectUri", required = false) String postLogoutRedirectUri,
                         HttpServletRequest request, HttpServletResponse response) {
        String userId = sessionService.destroySession(request, response);
        if (userId != null) {
            authService.revokeAllSessions(userId, request.getRemoteAddr());
        }
        cookieService.clearRefreshCookie(response);
        if (postLogoutRedirectUri != null && properties.isRegisteredRedirect(postLogoutRedirectUri)) {
            return "redirect:" + postLogoutRedirectUri;
        }
        return "forward:/sso/logout.html";
    }
}
