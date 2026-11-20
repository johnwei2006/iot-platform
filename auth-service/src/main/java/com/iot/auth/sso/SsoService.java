package com.iot.auth.sso;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 自建 SSO：一次性 ticket 在 Redis 中短期保存，绑定 clientId / redirectUri / userId，
 * 客户端凭 ticket + clientId 换取令牌；ticket 使用后立即删除，防止重放。
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

    public String consumeTicket(String ticket, String clientId) {
        String value = redis.opsForValue().get(ticketKey(ticket));
        if (value == null) {
            throw AppException.unauthorized("invalid or expired sso ticket");
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3 || !parts[0].equals(clientId)) {
            throw AppException.unauthorized("sso ticket client mismatch");
        }
        redis.delete(ticketKey(ticket));
        return parts[2];
    }

    private String ticketKey(String ticket) {
        return properties.getRedisPrefix() + "ticket:" + ticket;
    }
}
