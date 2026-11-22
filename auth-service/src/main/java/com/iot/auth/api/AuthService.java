package com.iot.auth.api;

import com.iot.auth.audit.AuditService;
import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import com.iot.auth.identity.User;
import com.iot.auth.identity.UserRepository;
import com.iot.auth.token.TokenClaims;
import com.iot.auth.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 认证业务：账号密码校验、令牌签发、刷新、登出、令牌校验、全局撤销、限流与审计。 */
@Service
public class AuthService {

    /** 用户版本/状态在 Redis 中的短缓存（verify 路径避免每次查 Mongo）。 */
    private static final long SESSION_STATE_CACHE_TTL_MS = 60_000L;

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthProperties properties;
    private final StringRedisTemplate redis;
    private final LoginRateLimiter rateLimiter;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AuthService(UserRepository userRepository, TokenService tokenService,
                       AuthProperties properties, StringRedisTemplate redis,
                       LoginRateLimiter rateLimiter, AuditService auditService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.properties = properties;
        this.redis = redis;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
    }

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request, String ip) {
        AuthProperties.Client client = properties.findClient(request.getClientId());
        if (client == null) {
            throw AppException.badRequest("unknown clientId: " + request.getClientId());
        }
        rateLimiter.check(request.getUsername(), ip);
        User user;
        try {
            user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> AppException.unauthorized("invalid username or password"));
        } catch (AppException e) {
            rateLimiter.recordFailure(request.getUsername(), ip);
            auditService.record(AuditService.LOGIN_FAILED, null, request.getUsername(),
                    request.getClientId(), ip, "unknown username");
            throw e;
        }
        if (!user.isEnabled()) {
            rateLimiter.recordFailure(request.getUsername(), ip);
            auditService.record(AuditService.LOGIN_FAILED, user.getId(), request.getUsername(),
                    request.getClientId(), ip, "user disabled");
            throw AppException.forbidden("user disabled");
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiter.recordFailure(request.getUsername(), ip);
            auditService.record(AuditService.LOGIN_FAILED, user.getId(), request.getUsername(),
                    request.getClientId(), ip, "wrong password");
            throw AppException.unauthorized("invalid username or password");
        }
        rateLimiter.recordSuccess(request.getUsername(), ip);
        AuthDtos.TokenResponse response = issueTokens(user, request.getClientId(), request.getDeviceId());
        auditService.record(AuditService.LOGIN_SUCCESS, user.getId(), request.getUsername(),
                request.getClientId(), ip, null);
        return response;
    }

    public AuthDtos.TokenResponse issueTokens(User user, String clientId, String deviceId) {
        user.setLastLoginAt(new Date());
        userRepository.save(user);

        String jti = UUID.randomUUID().toString();
        TokenClaims claims = new TokenClaims(user.getId(), clientId,
                new ArrayList<String>(user.getRoles()), jti, user.getTokenVersion(), deviceId);
        String access = tokenService.issueAccessToken(claims);
        String refresh = tokenService.issueRefreshToken(user.getId(), clientId,
                deviceId, user.getTokenVersion());

        AuthDtos.TokenResponse response = new AuthDtos.TokenResponse();
        response.setAccessToken(access);
        response.setRefreshToken(refresh);
        response.setExpiresInMs(properties.getAccessTtlMs());
        response.setUser(toUserInfo(user));
        return response;
    }

    /** SSO / 微信登录后按 userId 签发令牌；用户必须存在且启用。 */
    public AuthDtos.TokenResponse issueTokensById(String userId, String clientId, String deviceId) {
        AuthProperties.Client client = properties.findClient(clientId);
        if (client == null) {
            throw AppException.badRequest("unknown clientId: " + clientId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("user not found"));
        if (!user.isEnabled()) {
            throw AppException.forbidden("user disabled");
        }
        return issueTokens(user, clientId, deviceId);
    }

    /** 通过 openid 查找用户；不存在时返回 null，由调用方决定是否创建。 */
    public User findByOpenid(String openid) {
        return userRepository.findByOpenid(openid).orElse(null);
    }

    /** 创建微信用户（openid 首次登录）。 */
    public User createWechatUser(String openid, String unionid) {
        User user = new User();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        user.setRoles(new ArrayList<String>(java.util.Collections.singletonList("TERMINAL")));
        return userRepository.save(user);
    }

    /** 绑定手机号到已有用户。 */
    public void bindPhone(String userId, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("user not found"));
        user.setPhone(phone);
        userRepository.save(user);
    }

    /**
     * 刷新令牌：refresh 轮换（旧 token 作废、签发新 token）。
     * 检测到已轮换 token 被重用时，立即撤销该用户全部会话并拒绝。
     */
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request, String cookieRefresh) {
        String refreshToken = firstNonBlank(request == null ? null : request.getRefreshToken(), cookieRefresh);
        if (refreshToken == null) {
            throw AppException.unauthorized("missing refresh token");
        }
        TokenService.RefreshData data;
        try {
            data = tokenService.rotateRefresh(refreshToken);
        } catch (TokenService.RefreshReuseException e) {
            revokeAllSessions(e.getUserId(), null);
            auditService.record(AuditService.REFRESH_REUSE, e.getUserId(), null, null, null,
                    "rotated refresh token reuse detected; all sessions revoked");
            throw AppException.unauthorized("refresh token reuse detected; all sessions revoked");
        }
        User user = userRepository.findById(data.getUserId())
                .orElseThrow(() -> AppException.unauthorized("invalid refresh token"));
        if (!user.isEnabled()) {
            throw AppException.forbidden("user disabled");
        }
        if (user.getTokenVersion() != data.getVersion()) {
            throw AppException.unauthorized("refresh token revoked");
        }
        AuthDtos.TokenResponse response = new AuthDtos.TokenResponse();
        response.setAccessToken(tokenService.issueAccessFromRefresh(
                data, new ArrayList<String>(user.getRoles())));
        response.setRefreshToken(tokenService.issueRefreshToken(user.getId(),
                data.getClientId(), data.getDeviceId(), user.getTokenVersion()));
        response.setExpiresInMs(properties.getAccessTtlMs());
        return response;
    }

    /** 登出单个会话：撤销 refresh、拉黑 access。 */
    public void logout(AuthDtos.LogoutRequest request, String ip) {
        tokenService.revoke(request.getRefreshToken(), request.getAccessToken());
        if (request.getRefreshToken() != null || request.getAccessToken() != null) {
            auditService.record(AuditService.LOGOUT, null, null, null, ip, null);
        }
    }

    /**
     * 撤销该用户全部会话：递增 tokenVersion，使所有历史 access/refresh 立即失效。
     * 用于改密、禁用、管理员踢人与 refresh 重用响应。
     */
    public void revokeAllSessions(String userId, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("user not found"));
        user.setTokenVersion(user.getTokenVersion() + 1L);
        userRepository.save(user);
        redis.delete(sessionStateKey(userId));
        auditService.record(AuditService.REVOKE_ALL, userId, user.getUsername(), null, ip,
                "tokenVersion bumped to " + user.getTokenVersion());
    }

    /** 校验 access token：签名/过期/黑名单之外，再核对用户状态与 tokenVersion。 */
    public AuthDtos.VerifyResponse verify(String accessToken) {
        TokenClaims claims = tokenService.verifyAccessToken(accessToken);
        SessionState state = loadSessionState(claims.getSubject());
        if (!state.enabled) {
            throw AppException.unauthorized("user disabled");
        }
        if (state.tokenVersion != claims.getTokenVersion()) {
            throw AppException.unauthorized("token revoked");
        }
        AuthDtos.VerifyResponse response = new AuthDtos.VerifyResponse();
        response.setSubject(claims.getSubject());
        response.setClientId(claims.getClientId());
        response.setRoles(claims.getRoles());
        response.setTokenVersion(claims.getTokenVersion());
        response.setJti(claims.getJti());
        return response;
    }

    /** 用户版本/状态的 Redis 短缓存（"version|status"），revokeAllSessions 时主动失效。 */
    private SessionState loadSessionState(String userId) {
        String cached = redis.opsForValue().get(sessionStateKey(userId));
        if (cached != null) {
            String[] parts = cached.split("\\|", -1);
            if (parts.length == 2) {
                return new SessionState(Long.parseLong(parts[0]), "ENABLED".equals(parts[1]));
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.unauthorized("user not found"));
        redis.opsForValue().set(sessionStateKey(userId),
                user.getTokenVersion() + "|" + user.getStatus(),
                SESSION_STATE_CACHE_TTL_MS, TimeUnit.MILLISECONDS);
        return new SessionState(user.getTokenVersion(), user.isEnabled());
    }

    private String sessionStateKey(String userId) {
        return properties.getRedisPrefix() + "userver:" + userId;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    /** 用户会话状态快照。 */
    private static class SessionState {
        private final long tokenVersion;
        private final boolean enabled;

        SessionState(long tokenVersion, boolean enabled) {
            this.tokenVersion = tokenVersion;
            this.enabled = enabled;
        }
    }

    private AuthDtos.UserInfo toUserInfo(User user) {
        AuthDtos.UserInfo info = new AuthDtos.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRoles(new ArrayList<String>(user.getRoles()));
        return info;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
