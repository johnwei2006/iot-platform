package com.iot.auth.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 认证审计：关键认证事件落 MongoDB；写入失败仅告警，不影响主流程。 */
@Service
public class AuditService {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String LOGOUT = "LOGOUT";
    public static final String REVOKE_ALL = "REVOKE_ALL";
    public static final String REFRESH_REUSE = "REFRESH_REUSE";
    public static final String SSO_EXCHANGE = "SSO_EXCHANGE";

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuthEventRepository repository;

    @Autowired
    public AuditService(AuthEventRepository repository) {
        this.repository = repository;
    }

    public void record(String type, String userId, String username, String clientId, String ip, String detail) {
        try {
            AuthEvent event = new AuthEvent();
            event.setType(type);
            event.setUserId(userId);
            event.setUsername(username);
            event.setClientId(clientId);
            event.setIp(ip);
            event.setDetail(detail);
            repository.save(event);
        } catch (Exception e) {
            log.warn("Failed to persist audit event type={} userId={}: {}", type, userId, e.getMessage());
        }
    }
}
