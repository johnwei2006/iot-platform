package com.iot.auth.api;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import com.iot.auth.identity.User;
import com.iot.auth.identity.UserRepository;
import com.iot.auth.token.TokenClaims;
import com.iot.auth.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 认证业务：账号密码校验、令牌签发、刷新、登出、令牌校验。 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AuthService(UserRepository userRepository, TokenService tokenService, AuthProperties properties) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        AuthProperties.Client client = properties.findClient(request.getClientId());
        if (client == null) {
            throw AppException.badRequest("unknown clientId: " + request.getClientId());
        }
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> AppException.unauthorized("invalid username or password"));
        if (!user.isEnabled()) {
            throw AppException.forbidden("user disabled");
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.unauthorized("invalid username or password");
        }
        return issueTokens(user, request.getClientId(), request.getDeviceId());
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

    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        TokenService.RefreshData data = tokenService.readRefresh(request.getRefreshToken());
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
        response.setRefreshToken(request.getRefreshToken());
        response.setExpiresInMs(properties.getAccessTtlMs());
        return response;
    }

    public void logout(AuthDtos.LogoutRequest request) {
        tokenService.revoke(request.getRefreshToken(), request.getAccessToken());
    }

    public AuthDtos.VerifyResponse verify(String accessToken) {
        TokenClaims claims = tokenService.verifyAccessToken(accessToken);
        AuthDtos.VerifyResponse response = new AuthDtos.VerifyResponse();
        response.setSubject(claims.getSubject());
        response.setClientId(claims.getClientId());
        response.setRoles(claims.getRoles());
        response.setTokenVersion(claims.getTokenVersion());
        return response;
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
