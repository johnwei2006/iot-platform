package com.iot.auth.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一身份用户；口令只存 BCrypt 哈希，角色使用大写枚举值。
 *
 * <p>持久化到 MySQL sys_user（2026-11 由 Mongo users 集合迁移）；
 * 审计、SSO、refresh 等数据仍在 MongoDB/Redis。
 */
@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "username", length = 128, unique = true)
    private String username;

    /** BCrypt 哈希，禁止明文 */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "openid", length = 128, unique = true)
    private String openid;

    @Column(name = "unionid", length = 128, unique = true)
    private String unionid;

    @Column(name = "phone", length = 64, unique = true)
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles", columnDefinition = "JSON")
    private List<String> roles = new ArrayList<String>();

    /** 所属机构（admin-service sys_org 主键） */
    @Column(name = "org_id", length = 36)
    private String orgId;

    /** 所属机构编码（4-2-2-2 数字，层级前缀），冗余存储避免跨服务查树 */
    @Column(name = "org_code", length = 32)
    private String orgCode;

    /** 数据权限映射：业务实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF（由 admin-service 合并计算后同步） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_scopes", columnDefinition = "JSON")
    private Map<String, String> dataScopes = new HashMap<String, String>();

    /** 递增此值可让该用户所有历史 token 失效 */
    @Column(name = "token_version", nullable = false)
    private long tokenVersion = 0L;

    /** ENABLED / DISABLED */
    @Column(name = "status", length = 16)
    private String status = "ENABLED";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login_at")
    private Date lastLoginAt;

    @PrePersist
    void ensureId() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getUnionid() { return unionid; }
    public void setUnionid(String unionid) { this.unionid = unionid; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles == null ? new ArrayList<String>() : roles; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public Map<String, String> getDataScopes() { return dataScopes; }
    public void setDataScopes(Map<String, String> dataScopes) {
        this.dataScopes = dataScopes == null ? new HashMap<String, String>() : dataScopes;
    }
    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
