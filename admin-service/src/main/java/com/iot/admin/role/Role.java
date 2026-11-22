package com.iot.admin.role;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色：菜单授权（menuIds）+ 按业务实体的数据权限（dataScopes）。
 * dataScopes: 实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF。
 */
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    /** 角色编码（大写，如 SUPER_ADMIN / ORG_ADMIN / OPERATOR） */
    @Indexed(unique = true)
    private String code;

    private String name;

    /** 授权菜单（含 DIR/MENU/BUTTON 节点 id） */
    private List<String> menuIds = new ArrayList<String>();

    /** 按业务实体的数据权限 */
    private Map<String, String> dataScopes = new HashMap<String, String>();

    /** 内置角色（SUPER_ADMIN）：不可删除，数据权限不可修改 */
    private boolean builtIn = false;
    private String status = "ENABLED";
    private Date createdAt = new Date();

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
