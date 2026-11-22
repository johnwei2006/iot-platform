package com.iot.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * admin-service 配置。启动时 fail-fast 校验：
 * HMAC 服务密钥必须配置（与 auth-service 的 AUTH_SERVICE_ADMIN_SECRET 一致）。
 */
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    private Auth auth = new Auth();
    /** 数据权限业务实体注册表：新增实体即新增配置行 */
    private List<Entity> entities = new ArrayList<Entity>();
    private Bootstrap bootstrap = new Bootstrap();

    @PostConstruct
    public void validate() {
        if (auth.getServiceSecret() == null || auth.getServiceSecret().trim().isEmpty()) {
            throw new IllegalStateException("admin.auth.service-secret (AUTH_SERVICE_ADMIN_SECRET) must be configured");
        }
        if (auth.getServiceId() == null || auth.getServiceId().trim().isEmpty()) {
            throw new IllegalStateException("admin.auth.service-id must be configured");
        }
    }

    public Entity findEntity(String code) {
        if (code == null) {
            return null;
        }
        for (Entity entity : entities) {
            if (code.equals(entity.getCode())) {
                return entity;
            }
        }
        return null;
    }

    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth == null ? new Auth() : auth; }
    public List<Entity> getEntities() { return entities; }
    public void setEntities(List<Entity> entities) { this.entities = entities; }
    public Bootstrap getBootstrap() { return bootstrap; }
    public void setBootstrap(Bootstrap bootstrap) { this.bootstrap = bootstrap == null ? new Bootstrap() : bootstrap; }

    public static class Auth {
        private String baseUrl;
        private String jwksUrl;
        private String issuer;
        private String audience;
        private String serviceId;
        private String serviceSecret;
        private Revocation revocation = new Revocation();

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getJwksUrl() { return jwksUrl; }
        public void setJwksUrl(String jwksUrl) { this.jwksUrl = jwksUrl; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        public String getServiceSecret() { return serviceSecret; }
        public void setServiceSecret(String serviceSecret) { this.serviceSecret = serviceSecret; }
        public Revocation getRevocation() { return revocation; }
        public void setRevocation(Revocation revocation) { this.revocation = revocation == null ? new Revocation() : revocation; }
    }

    public static class Revocation {
        private boolean enabled = true;
        private String redisPrefix = "auth:";
        private long cacheTtlMs = 15_000L;
        private boolean failOpen = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getRedisPrefix() { return redisPrefix; }
        public void setRedisPrefix(String redisPrefix) { this.redisPrefix = redisPrefix; }
        public long getCacheTtlMs() { return cacheTtlMs; }
        public void setCacheTtlMs(long cacheTtlMs) { this.cacheTtlMs = cacheTtlMs; }
        public boolean isFailOpen() { return failOpen; }
        public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }
    }

    /** 数据权限业务实体（code 与业务服务 DataScopeRegistry 约定一致）。 */
    public static class Entity {
        private String code;
        private String name;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Bootstrap {
        private String adminUsername = "admin";
        private String adminPassword = "Admin@123456";
        private String rootOrgCode = "0001";
        private String rootOrgName = "总部";

        public String getAdminUsername() { return adminUsername; }
        public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
        public String getRootOrgCode() { return rootOrgCode; }
        public void setRootOrgCode(String rootOrgCode) { this.rootOrgCode = rootOrgCode; }
        public String getRootOrgName() { return rootOrgName; }
        public void setRootOrgName(String rootOrgName) { this.rootOrgName = rootOrgName; }
    }
}
