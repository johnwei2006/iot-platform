package com.iot.auth.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.auth.api.AuthDtos;
import com.iot.auth.api.AuthService;
import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import com.iot.auth.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 微信小程序登录：后端用 AppID + AppSecret 调微信 jscode2session，前端 never 持有 AppSecret。
 *
 * <p>手机号绑定使用微信新规的 getPhoneNumber code，调用 phonenumber/getuserphonenumber
 * 换取手机号。两个接口都必须服务端调用，token 不下发前端。
 */
@Service
public class WechatService {

    private static final Logger log = LoggerFactory.getLogger(WechatService.class);
    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";
    private static final String GET_PHONE_URL =
            "https://api.weixin.qq.com/cgi-bin/phonenumber/getuserphonenumber";

    private final AuthProperties properties;
    private final AuthService authService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WechatService(AuthProperties properties, AuthService authService, RestTemplate restTemplate) {
        this.properties = properties;
        this.authService = authService;
        this.restTemplate = restTemplate;
    }

    /** wx.login 的 code 换取本平台令牌；首次登录自动创建 TERMINAL 用户。 */
    public AuthDtos.TokenResponse login(String code, String clientId, String deviceId) {
        requireWechatConfig();
        JsonNode session = callJscode2Session(code);
        String openid = text(session, "openid");
        if (openid == null || openid.isEmpty()) {
            String errcode = text(session, "errcode");
            String errmsg = text(session, "errmsg");
            log.warn("wechat jscode2session failed: errcode={} errmsg={}", errcode, errmsg);
            throw AppException.unauthorized("wechat login failed: " + errmsg);
        }
        String unionid = text(session, "unionid");
        User user = authService.findByOpenid(openid);
        if (user == null) {
            user = authService.createWechatUser(openid, unionid);
        }
        return authService.issueTokensById(user.getId(),
                clientId == null ? "mini" : clientId, deviceId);
    }

    /** getPhoneNumber 的 code 换取手机号并绑定当前用户。 */
    public void bindPhone(String userId, String phoneCode) {
        requireWechatConfig();
        String accessToken = fetchAccessToken();
        String url = UriComponentsBuilder.fromHttpUrl(GET_PHONE_URL)
                .queryParam("access_token", accessToken)
                .toUriString();
        String body = "{\"code\":\"" + escapeJson(phoneCode) + "\"}";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(url,
                new org.springframework.http.HttpEntity<String>(body, headers), String.class);
        JsonNode json;
        try {
            json = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw AppException.badRequest("invalid wechat phone response");
        }
        if (json.path("errcode").asInt(0) != 0) {
            throw AppException.badRequest("wechat phone bind failed: " + json.path("errmsg").asText());
        }
        String phone = json.path("phone_info").path("purePhoneNumber").asText(null);
        if (phone == null || phone.isEmpty()) {
            phone = json.path("phone_info").path("phoneNumber").asText(null);
        }
        if (phone == null || phone.isEmpty()) {
            throw AppException.badRequest("wechat phone not returned");
        }
        authService.bindPhone(userId, phone);
    }

    private JsonNode callJscode2Session(String code) {
        String url = UriComponentsBuilder.fromHttpUrl(JSCODE2SESSION_URL)
                .queryParam("appid", properties.getWechat().getAppId())
                .queryParam("secret", properties.getWechat().getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw AppException.badRequest("invalid wechat session response");
        }
    }

    private String fetchAccessToken() {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", properties.getWechat().getAppId())
                .queryParam("secret", properties.getWechat().getAppSecret())
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            String token = text(json, "access_token");
            if (token == null || token.isEmpty()) {
                throw AppException.badRequest("wechat access_token unavailable");
            }
            return token;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.badRequest("invalid wechat token response");
        }
    }

    private void requireWechatConfig() {
        AuthProperties.Wechat wechat = properties.getWechat();
        if (wechat == null || wechat.getAppId() == null || wechat.getAppId().trim().isEmpty()
                || wechat.getAppSecret() == null || wechat.getAppSecret().trim().isEmpty()) {
            throw AppException.badRequest("wechat appId/appSecret not configured");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
