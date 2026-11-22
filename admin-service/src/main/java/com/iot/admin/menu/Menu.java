package com.iot.admin.menu;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 菜单/按钮统一树节点：DIR（目录）/ MENU（菜单，挂组件路径）/ BUTTON（按钮，挂权限标识）。
 */
@Document(collection = "menus")
public class Menu {

    public static final String TYPE_DIR = "DIR";
    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_BUTTON = "BUTTON";

    @Id
    private String id;

    /** 父菜单 id；根节点为 null */
    private String parentId;

    private String name;

    /** DIR / MENU / BUTTON */
    private String type;

    /** 路由路径（MENU） */
    private String path;

    /** 组件路径，如 system/org/index（MENU） */
    private String component;

    /** 权限标识（BUTTON），如 system:org:create */
    @org.springframework.data.mongodb.core.index.Indexed(unique = true, sparse = true)
    private String perm;

    private String icon;
    private String sort = "0";
    private String status = "ENABLED";
    private Date createdAt = new Date();

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
