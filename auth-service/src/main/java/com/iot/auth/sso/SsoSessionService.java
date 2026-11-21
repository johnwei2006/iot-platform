package com.iot.auth.sso;

import com.iot.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SSO 中央会话：统一登录页登录成功后建立，浏览器后续跨客户端换票免密。
 *
 * <p>会话 id 为不透明随机串，存 Redis（绑定 userId），以 HttpOnly Cookie
 * 携带在 auth-service 域内（登录页与 /api/sso/* 同站）。单点登出销毁会话
 * 并递增 tokenVersion 撤销所有已签发令牌。
 */
@Service
public class SsoSessionService {

    private final AuthProperties properties;
    private final StringRedisTemplate redis;

    @Autowired
    public SsoSessionService(AuthProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    /** 创建中央会话并写入 Cookie。 */
    public void createSession(String userId, HttpServletResponse response) {
        AuthProperties.Sso cfg = properties.getSso();
        if (cfg == null || !cfg.isSessionEnabled() || userId == null || userId.isEmpty()) {
            return;
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(sessionKey(sessionId), userId,
                cfg.getSessionTtlMs(), TimeUnit.MILLISECONDS);
        writeCookie(response, cfg.getSessionCookieName(), sessionId,
                cfg.getSessionTtlMs() / 1000L, cfg);
    }

    /** 从 Cookie/Redis 解析当前会话用户；不存在或过期返回 null。 */
    public String resolveUserId(HttpServletRequest request) {
        AuthProperties.Sso cfg = properties.getSso();
        if (cfg == null || !cfg.isSessionEnabled() || request == null) {
            return null;
        }
        String sessionId = readCookie(request, cfg.getSessionCookieName());
        if (sessionId == null) {
            return null;
        }
        return redis.opsForValue().get(sessionKey(sessionId));
    }

    /** 销毁当前会话并清除 Cookie；返回被销毁会话的 userId（可能为 null）。 */
    public String destroySession(HttpServletRequest request, HttpServletResponse response) {
        AuthProperties.Sso cfg = properties.getSso();
        if (cfg == null || !cfg.isSessionEnabled()) {
            return null;
        }
        String sessionId = readCookie(request, cfg.getSessionCookieName());
        String userId = null;
        if (sessionId != null) {
            userId = redis.opsForValue().get(sessionKey(sessionId));
            redis.delete(sessionKey(sessionId));
        }
        writeCookie(response, cfg.getSessionCookieName(), "", 0, cfg);
        return userId;
    }

    private String sessionKey(String sessionId) {
        return properties.getRedisPrefix() + "sso-session:" + sessionId;
    }

    private String readCookie(HttpServletRequest request, String name) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (javax.servlet.http.Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String name, String value,
                             long maxAgeSeconds, AuthProperties.Sso cfg) {
        StringBuilder header = new StringBuilder();
        header.append(name).append('=').append(value);
        header.append("; Path=/");
        header.append("; Max-Age=").append(maxAgeSeconds);
        header.append("; HttpOnly");
        if (cfg.isSessionCookieSecure()) {
            header.append("; Secure");
        }
        if (cfg.getSessionCookieSameSite() != null && !cfg.getSessionCookieSameSite().trim().isEmpty()) {
            header.append("; SameSite=").append(cfg.getSessionCookieSameSite().trim());
        }
        if (cfg.getSessionCookieDomain() != null && !cfg.getSessionCookieDomain().trim().isEmpty()) {
            header.append("; Domain=").append(cfg.getSessionCookieDomain().trim());
        }
        response.addHeader("Set-Cookie", header.toString());
    }
}
