package com.iot.admin.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iot.admin.common.JsonTypeHandlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色：菜单授权（menuIds）+ 按业务实体的数据权限（dataScopes），两者以 MySQL JSON 列存储。
 */
@TableName(value = "sys_role", autoResultMap = true)
public class Role {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    /** 角色编码（大写，如 SUPER_ADMIN） */
    private String code;

    private String name;

    /** 授权菜单（含 DIR/MENU/BUTTON 节点 id） */
    @TableField(value = "menu_ids", typeHandler = JsonTypeHandlers.ListStringTypeHandler.class)
    private List<String> menuIds = new ArrayList<String>();

    /** 按业务实体的数据权限：实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF */
    @TableField(value = "data_scopes", typeHandler = JsonTypeHandlers.MapStringTypeHandler.class)
    private Map<String, String> dataScopes = new HashMap<String, String>();

    /** 内置角色不可删除 */
    @TableField("built_in")
    private boolean builtIn = false;

    private String status = "ENABLED";

    @TableField("created_at")
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
