package com.iot.admin.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;
import java.util.UUID;

/**
 * 菜单/按钮统一树节点：DIR（目录）/ MENU（菜单，挂组件路径）/ BUTTON（按钮，挂权限标识）。
 *
 * <p>持久化到 MySQL sys_menu；按钮仍以 type=BUTTON 的节点存储。
 */
@Entity
@Table(name = "sys_menu", indexes = {
        @Index(name = "idx_sys_menu_parent_id", columnList = "parent_id"),
        @Index(name = "idx_sys_menu_path", columnList = "path")
})
public class Menu {

    public static final String TYPE_DIR = "DIR";
    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_BUTTON = "BUTTON";

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /** 父菜单 id；根节点为 null */
    @Column(name = "parent_id", length = 36)
    private String parentId;

    private String name;

    /** DIR / MENU / BUTTON */
    @Column(name = "type", length = 16, nullable = false)
    private String type;

    /** 路由路径（MENU） */
    private String path;

    /** 组件路径，如 system/org/index（MENU） */
    private String component;

    /** 权限标识（BUTTON），如 system:org:create；MySQL 唯一索引允许多个 NULL */
    @Column(name = "perm", length = 128, unique = true)
    private String perm;

    private String icon;

    @Column(name = "sort", length = 16)
    private String sort = "0";

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
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getPerm() { return perm; }
    public void setPerm(String perm) { this.perm = perm; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
