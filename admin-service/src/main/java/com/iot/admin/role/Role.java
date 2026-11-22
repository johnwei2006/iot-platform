package com.iot.admin.role;

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
 * 角色：菜单授权（menuIds）+ 按业务实体的数据权限（dataScopes）。
 * dataScopes: 实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF。
 *
 * <p>持久化到 MySQL sys_role；集合字段以 JSON 列整体读写（不做 SQL 关联查询）。
 */
@Entity
@Table(name = "sys_role")
public class Role {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /** 角色编码（大写，如 SUPER_ADMIN / ORG_ADMIN / OPERATOR） */
    @Column(name = "code", length = 64, nullable = false, unique = true)
    private String code;

    private String name;

    /** 授权菜单（含 DIR/MENU/BUTTON 节点 id） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "menu_ids", columnDefinition = "JSON")
    private List<String> menuIds = new ArrayList<String>();

    /** 按业务实体的数据权限 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_scopes", columnDefinition = "JSON")
    private Map<String, String> dataScopes = new HashMap<String, String>();

    /** 内置角色（SUPER_ADMIN）：不可删除，数据权限不可修改 */
    @Column(name = "built_in", nullable = false)
    private boolean builtIn = false;

    @Column(name = "status", length = 16)
    private String status = "ENABLED";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @PrePersist
    void ensureId() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getMenuIds() { return menuIds; }
    public void setMenuIds(List<String> menuIds) { this.menuIds = menuIds == null ? new ArrayList<String>() : menuIds; }
    public Map<String, String> getDataScopes() { return dataScopes; }
    public void setDataScopes(Map<String, String> dataScopes) {
        this.dataScopes = dataScopes == null ? new HashMap<String, String>() : dataScopes;
    }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
