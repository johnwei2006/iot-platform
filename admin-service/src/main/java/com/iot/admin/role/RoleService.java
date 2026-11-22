package com.iot.admin.role;

import com.iot.admin.common.AppException;
import com.iot.admin.config.AdminProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色服务：菜单授权 + 按业务实体的四级数据权限（ALL > ORG_AND_CHILDREN > ORG_ONLY > SELF）。
 * 多角色合并时每实体取最宽范围；未配置的已知实体默认 SELF（最严格）。
 * 扩展新业务实体：admin.entities 配置注册后，各角色的 dataScopes 即可为该实体配置范围。
 */
@Service
public class RoleService {

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_ORG_AND_CHILDREN = "ORG_AND_CHILDREN";
    public static final String SCOPE_ORG_ONLY = "ORG_ONLY";
    public static final String SCOPE_SELF = "SELF";
    public static final List<String> VALID_SCOPES = Arrays.asList(
            SCOPE_ALL, SCOPE_ORG_AND_CHILDREN, SCOPE_ORG_ONLY, SCOPE_SELF);

    private static final Map<String, Integer> SCOPE_WIDTH = new LinkedHashMap<String, Integer>();
    static {
        SCOPE_WIDTH.put(SCOPE_ALL, 3);
        SCOPE_WIDTH.put(SCOPE_ORG_AND_CHILDREN, 2);
        SCOPE_WIDTH.put(SCOPE_ORG_ONLY, 1);
        SCOPE_WIDTH.put(SCOPE_SELF, 0);
    }

    private final RoleRepository roleRepository;
    private final AdminProperties properties;

    @Autowired
    public RoleService(RoleRepository roleRepository, AdminProperties properties) {
        this.roleRepository = roleRepository;
        this.properties = properties;
    }

    public Role create(String code, String name, List<String> menuIds,
                       Map<String, String> dataScopes) {
        validateScopes(dataScopes);
        if (roleRepository.findByCode(code).isPresent()) {
            throw AppException.conflict("role code already exists: " + code);
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setMenuIds(menuIds);
        role.setDataScopes(dataScopes);
        return roleRepository.save(role);
    }

    public Role update(String id, String name, List<String> menuIds, Map<String, String> dataScopes,
                       String status) {
        Role role = requireRole(id);
        if (role.isBuiltIn()) {
            // 内置角色仅允许改名，菜单与数据权限锁定
            if (menuIds != null || dataScopes != null) {
                throw AppException.forbidden("built-in role is locked: " + role.getCode());
            }
        }
        if (dataScopes != null) {
            validateScopes(dataScopes);
            role.setDataScopes(dataScopes);
        }
        if (menuIds != null) {
            role.setMenuIds(menuIds);
        }
        if (name != null) {
            role.setName(name);
        }
        if (status != null && ("ENABLED".equals(status) || "DISABLED".equals(status))) {
            role.setStatus(status);
        }
        return roleRepository.save(role);
    }

    /** 删除角色（内置角色不可删）。 */
    public void delete(String id) {
        Role role = requireRole(id);
        if (role.isBuiltIn()) {
            throw AppException.forbidden("built-in role cannot be deleted: " + role.getCode());
        }
        roleRepository.delete(role);
    }

    public List<Role> listAll() {
        return roleRepository.findAll();
    }

    public List<Role> findByCodes(List<String> codes) {
        List<Role> roles = new ArrayList<Role>();
        for (Role role : roleRepository.findAll()) {
            if (codes.contains(role.getCode())) {
                roles.add(role);
            }
        }
        return roles;
    }

    /**
     * 合并多角色的数据权限：每业务实体取最宽范围；已知实体未配置默认 SELF。
     * 这是"数据权限角色"体系的核心计算，结果随 JWT ds 声明下发到业务服务。
     */
    public Map<String, String> mergeDataScopes(List<Role> roles) {
        Set<String> entities = new HashSet<String>();
        for (AdminProperties.Entity entity : properties.getEntities()) {
            entities.add(entity.getCode());
        }
        Map<String, String> merged = new HashMap<String, String>();
        for (Role role : roles) {
            if (role.getDataScopes() == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : role.getDataScopes().entrySet()) {
                entities.add(entry.getKey());
                String current = merged.get(entry.getKey());
                if (current == null || SCOPE_WIDTH.getOrDefault(entry.getValue(), 0)
                        > SCOPE_WIDTH.getOrDefault(current, 0)) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }
        Map<String, String> result = new LinkedHashMap<String, String>();
        List<String> ordered = new ArrayList<String>(entities);
        java.util.Collections.sort(ordered);
        for (String entity : ordered) {
            result.put(entity, merged.getOrDefault(entity, SCOPE_SELF));
        }
        return result;
    }

    private void validateScopes(Map<String, String> dataScopes) {
        if (dataScopes == null) {
            return;
        }
        for (Map.Entry<String, String> entry : dataScopes.entrySet()) {
            if (properties.findEntity(entry.getKey()) == null) {
                throw AppException.badRequest("unknown data-scope entity: " + entry.getKey()
                        + " (register it in admin.entities first)");
            }
            if (!VALID_SCOPES.contains(entry.getValue())) {
                throw AppException.badRequest("invalid data scope for entity "
                        + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    private Role requireRole(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("role not found: " + id));
    }
}
