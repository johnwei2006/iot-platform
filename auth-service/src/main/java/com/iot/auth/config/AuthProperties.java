package com.iot.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 认证中心配置；必填项在启动阶段校验，缺失即 fail-fast。
 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** JWT 密钥（HS256 兼容模式），至少 32 字符；未配置私钥时必填 */
    private String jwtSecret;
    /**
     * JWT 签名私钥（RS256，PKCS#8 PEM，openssl genpkey 输出格式）。
     * 配置后优先于 jwt-secret，启用非对称签名：下游服务持公钥本地验签，无需共享秘密。
     */
    private String jwtPrivateKey;
    /** JWT kid 头（RS256 模式），配合 JWKS 端点支持密钥轮换 */
    private String jwtKeyId = "auth-key-1";
    /** JWT iss 声明 */
    private String jwtIssuer = "iot-auth";
    /** JWT aud 声明 */
    private String jwtAudience = "iot-platform";
    /** 登录失败限流 */
    private RateLimit rateLimit = new RateLimit();
    /** access token 有效期（毫秒），默认 30 分钟 */
    private long accessTtlMs = 30L * 60 * 1000;
    /** refresh token 有效期（毫秒），默认 7 天 */
    private long refreshTtlMs = 7L * 24 * 60 * 60 * 1000;
    /** SSO ticket 有效期（毫秒），默认 5 分钟 */
    private long ticketTtlMs = 5L * 60 * 1000;
    /** Redis key 前缀 */
    private String redisPrefix = "auth:";
    /** 可信 SSO 客户端 */
    private List<Client> clients = new ArrayList<Client>();
    /** 内部服务 HMAC 密钥 */
    private List<ServiceKey> serviceKeys = new ArrayList<ServiceKey>();
    /** 微信小程序凭证 */
    private Wechat wechat = new Wechat();
    /** Refresh Token HttpOnly Cookie */
    private Cookie cookie = new Cookie();
    /** SSO 中央会话（统一登录页） */
    private Sso sso = new Sso();

    @PostConstruct
    public void validate() {
        boolean hasRsa = jwtPrivateKey != null && jwtPrivateKey.contains("PRIVATE KEY");
        boolean hasHmac = jwtSecret != null && jwtSecret.length() >= 32;
        if (!hasRsa && !hasHmac) {
            throw new IllegalStateException("auth.jwt-private-key (RS256, PKCS#8 PEM) or auth.jwt-secret "
                    + "(HS256, at least 32 characters) must be configured");
        }
    }

    /** 是否启用 RS256 非对称签名模式（配置私钥时优先）。 */
    public boolean isRsaMode() {
        return jwtPrivateKey != null && jwtPrivateKey.contains("PRIVATE KEY");
    }

    public String getJwtPrivateKey() { return jwtPrivateKey; }
    public void setJwtPrivateKey(String jwtPrivateKey) { this.jwtPrivateKey = jwtPrivateKey; }
    public String getJwtKeyId() { return jwtKeyId; }
    public void setJwtKeyId(String jwtKeyId) { this.jwtKeyId = jwtKeyId; }
    public String getJwtIssuer() { return jwtIssuer; }
    public void setJwtIssuer(String jwtIssuer) { this.jwtIssuer = jwtIssuer; }
    public String getJwtAudience() { return jwtAudience; }
    public void setJwtAudience(String jwtAudience) { this.jwtAudience = jwtAudience; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit == null ? new RateLimit() : rateLimit; }

    public Client findClient(String clientId) {
        for (Client client : clients) {
            if (client != null && clientId != null && clientId.equals(client.getId())) {
                return client;
            }
        }
        return null;
    }

    public String findServiceSecret(String serviceId) {
        for (ServiceKey key : serviceKeys) {
            if (key != null && serviceId != null && serviceId.equals(key.getId())) {
                return key.getSecret();
            }
        }
        return null;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessTtlMs() {
        return accessTtlMs;
    }

    public void setAccessTtlMs(long accessTtlMs) {
        this.accessTtlMs = accessTtlMs;
    }

    public long getRefreshTtlMs() {
        return refreshTtlMs;
    }

    public void setRefreshTtlMs(long refreshTtlMs) {
        this.refreshTtlMs = refreshTtlMs;
    }

    public long getTicketTtlMs() {
        return ticketTtlMs;
    }

    public void setTicketTtlMs(long ticketTtlMs) {
        this.ticketTtlMs = ticketTtlMs;
    }

    public String getRedisPrefix() {
        return redisPrefix;
    }

    public void setRedisPrefix(String redisPrefix) {
        this.redisPrefix = redisPrefix;
    }

    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public void setClients(List<Client> clients) {
        this.clients = clients == null ? new ArrayList<Client>() : clients;
    }

    public List<ServiceKey> getServiceKeys() {
        return Collections.unmodifiableList(serviceKeys);
    }

    public void setServiceKeys(List<ServiceKey> serviceKeys) {
        this.serviceKeys = serviceKeys == null ? new ArrayList<ServiceKey>() : serviceKeys;
    }

    public Wechat getWechat() {
        return wechat;
    }

    public void setWechat(Wechat wechat) {
        this.wechat = wechat == null ? new Wechat() : wechat;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie == null ? new Cookie() : cookie;
    }

    public Sso getSso() {
        return sso;
    }

    public void setSso(Sso sso) {
        this.sso = sso == null ? new Sso() : sso;
    }

    /** 是否为任一客户端注册过的 redirectUri（用于登出跳转校验）。 */
    public boolean isRegisteredRedirect(String uri) {
        if (uri == null) {
            return false;
        }
        for (Client client : clients) {
            if (client != null && client.allowsRedirect(uri)) {
                return true;
            }
        }
        return false;
    }

    /** Refresh Token Cookie 设置；enabled=false 时不写/不读 Cookie（纯 body 模式）。 */
    public static class Cookie {
        /** 是否启用 HttpOnly Cookie 承载 refresh token */
        private boolean enabled = true;
        private String name = "auth_refresh";
        /** 生效路径：只需覆盖签发/刷新/登出端点 */
        private String path = "/api/auth";
        /** 可选 Domain；跨域部署时留空 */
        private String domain = "";
        /** 生产 HTTPS 环境必须开启 */
        private boolean secure = false;
        /** SameSite 属性：Strict / Lax / None */
        private String sameSite = "Strict";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }
    }

    /**
     * SSO 中央会话：统一登录页登录成功后建立，浏览器后续免密换票。
     * Cookie 作用域为 auth-service 域内（登录页与 /api/sso/* 同站）。
     */
    public static class Sso {
        /** 是否启用中央会话 */
        private boolean sessionEnabled = true;
        /** 中央会话有效期（毫秒），默认 8 小时 */
        private long sessionTtlMs = 8L * 60 * 60 * 1000;
        private String sessionCookieName = "auth_sso";
        /** 可选 Domain；跨域部署时留空 */
        private String sessionCookieDomain = "";
        /** 生产 HTTPS 环境必须开启 */
        private boolean sessionCookieSecure = false;
        /** 登录页与 API 同站跳转，Lax 足够 */
        private String sessionCookieSameSite = "Lax";

        public boolean isSessionEnabled() { return sessionEnabled; }
        public void setSessionEnabled(boolean sessionEnabled) { this.sessionEnabled = sessionEnabled; }
        public long getSessionTtlMs() { return sessionTtlMs; }
        public void setSessionTtlMs(long sessionTtlMs) { this.sessionTtlMs = sessionTtlMs; }
        public String getSessionCookieName() { return sessionCookieName; }
        public void setSessionCookieName(String sessionCookieName) { this.sessionCookieName = sessionCookieName; }
        public String getSessionCookieDomain() { return sessionCookieDomain; }
        public void setSessionCookieDomain(String sessionCookieDomain) { this.sessionCookieDomain = sessionCookieDomain; }
        public boolean isSessionCookieSecure() { return sessionCookieSecure; }
        public void setSessionCookieSecure(boolean sessionCookieSecure) { this.sessionCookieSecure = sessionCookieSecure; }
        public String getSessionCookieSameSite() { return sessionCookieSameSite; }
        public void setSessionCookieSameSite(String sessionCookieSameSite) { this.sessionCookieSameSite = sessionCookieSameSite; }
    }

    /** 登录失败限流：同一用户名/来源 IP 在窗口内失败次数超限即拒绝（429）。 */
    public static class RateLimit {
        private boolean enabled = true;
        /** 窗口内最大失败次数 */
        private int maxFailures = 10;
        /** 计数窗口（毫秒），默认 15 分钟 */
        private long windowMs = 15L * 60 * 1000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxFailures() { return maxFailures; }
        public void setMaxFailures(int maxFailures) { this.maxFailures = maxFailures; }
        public long getWindowMs() { return windowMs; }
        public void setWindowMs(long windowMs) { this.windowMs = windowMs; }
    }

    public static class Client {
        private String id;
        private String secret;
        private List<String> redirectUris = new ArrayList<String>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris == null ? new ArrayList<String>() : redirectUris;
        }
        public boolean allowsRedirect(String uri) {
            return uri != null && redirectUris.contains(uri);
        }
    }

    public static class ServiceKey {
        private String id;
        private String secret;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }

    public static class Wechat {
        /** 小程序 AppID */
        private String appId;
        /** 小程序 AppSecret（仅后端持有） */
        private String appSecret;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    }
}
