package com.iot.auth.token;

import com.iot.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Refresh Token 的 HttpOnly Cookie 承载。
 *
 * <p>浏览器端 refresh token 不落 localStorage，改由 HttpOnly Cookie 自动携带，
 * 降低 XSS 窃取长期凭证的风险；小程序/原生客户端仍可使用响应体中的 refreshToken。
 * Servlet Cookie API 不支持 SameSite，因此直接写 Set-Cookie 头。
 */
@Component
public class RefreshCookieService {

    private final AuthProperties properties;

    @Autowired
    public RefreshCookieService(AuthProperties properties) {
        this.properties = properties;
    }

    /** 在响应中写入 refresh cookie；有效期与 refresh TTL 一致。 */
    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        AuthProperties.Cookie cfg = properties.getCookie();
        if (cfg == null || !cfg.isEnabled() || refreshToken == null || refreshToken.isEmpty()) {
            return;
        }
        StringBuilder header = new StringBuilder();
        header.append(cfg.getName()).append('=').append(refreshToken);
        header.append("; Path=").append(cfg.getPath());
        header.append("; Max-Age=").append(properties.getRefreshTtlMs() / 1000L);
        header.append("; HttpOnly");
        if (cfg.isSecure()) {
            header.append("; Secure");
        }
        if (cfg.getSameSite() != null && !cfg.getSameSite().trim().isEmpty()) {
            header.append("; SameSite=").append(cfg.getSameSite().trim());
        }
        if (cfg.getDomain() != null && !cfg.getDomain().trim().isEmpty()) {
            header.append("; Domain=").append(cfg.getDomain().trim());
        }
        response.addHeader("Set-Cookie", header.toString());
    }

    /** 清除 refresh cookie（登出 / 会话失效时调用）。 */
    public void clearRefreshCookie(HttpServletResponse response) {
        AuthProperties.Cookie cfg = properties.getCookie();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        StringBuilder header = new StringBuilder();
        header.append(cfg.getName()).append('=');
        header.append("; Path=").append(cfg.getPath());
        header.append("; Max-Age=0");
        header.append("; HttpOnly");
        if (cfg.isSecure()) {
            header.append("; Secure");
        }
        if (cfg.getSameSite() != null && !cfg.getSameSite().trim().isEmpty()) {
            header.append("; SameSite=").append(cfg.getSameSite().trim());
        }
        if (cfg.getDomain() != null && !cfg.getDomain().trim().isEmpty()) {
            header.append("; Domain=").append(cfg.getDomain().trim());
        }
        response.addHeader("Set-Cookie", header.toString());
    }

    /** 从请求 Cookie 中读取 refresh token；不存在返回 null。 */
    public String extractRefreshCookie(HttpServletRequest request) {
        AuthProperties.Cookie cfg = properties.getCookie();
        if (cfg == null || !cfg.isEnabled() || request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cfg.getName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
