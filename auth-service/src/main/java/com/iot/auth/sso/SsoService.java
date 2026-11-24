package com.iot.auth.sso;

import com.iot.api.auth.AppException;
import com.iot.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 自建 SSO：一次性 ticket 在 Redis 中短期保存，绑定 clientId / redirectUri / userId，
 * 客户端凭 ticket + clientId + clientSecret 换取令牌；ticket 使用后立即删除，防止重放。
 * 兑票时校验 clientSecret 与 redirectUri 绑定，防止票据被非注册客户端或非注册回调消费。
 */
@Service
public class SsoService {

    private final AuthProperties properties;
    private final StringRedisTemplate redis;

    @Autowired
    public SsoService(AuthProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    public String issueTicket(String clientId, String redirectUri, String userId) {
        AuthProperties.Client client = properties.findClient(clientId);
        if (client == null) {
            throw AppException.badRequest("unknown clientId: " + clientId);
        }
        if (!client.allowsRedirect(redirectUri)) {
            throw AppException.badRequest("redirectUri not allowed for client: " + clientId);
        }
        String ticket = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(ticketKey(ticket), clientId + "|" + redirectUri + "|" + userId,
                properties.getTicketTtlMs(), TimeUnit.MILLISECONDS);
        return ticket;
    }

    /**
     * 兑票：先校验 clientSecret（客户端已配置密钥时必须匹配），再校验 redirectUri 与票内绑定一致，
     * 通过后消费 ticket（一次性）。校验失败不消费 ticket。
     */
    public String consumeTicket(String ticket, String clientId, String clientSecret, String redirectUri) {
        AuthProperties.Client client = properties.findClient(clientId);
        if (client == null) {
            throw AppException.badRequest("unknown clientId: " + clientId);
        }
        if (client.getSecret() != null && !client.getSecret().trim().isEmpty()
                && !constantTimeEquals(clientSecret, client.getSecret())) {
            throw AppException.unauthorized("client secret mismatch");
        }
        String value = redis.opsForValue().get(ticketKey(ticket));
        if (value == null) {
            throw AppException.unauthorized("invalid or expired sso ticket");
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3 || !parts[0].equals(clientId)) {
            throw AppException.unauthorized("sso ticket client mismatch");
        }
        if (redirectUri == null || !redirectUri.equals(parts[1])) {
            throw AppException.unauthorized("redirectUri mismatch");
        }
        redis.delete(ticketKey(ticket));
        return parts[2];
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String ticketKey(String ticket) {
        return properties.getRedisPrefix() + "ticket:" + ticket;
    }
}
