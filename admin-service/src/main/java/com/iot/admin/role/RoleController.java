package com.iot.admin.role;

import com.iot.admin.common.ApiResponse;
import com.iot.admin.config.AdminProperties;
import com.iot.admin.product.ProductService;
import com.iot.admin.security.RequireRoles;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 角色管理：平台超管全局授权；机构管理员只能在本机构产品范围内勾选菜单。 */
@RestController
@RequestMapping("/api/admin/roles")
@RequireRoles({"SUPER_ADMIN", "ORG_ADMIN"})
public class RoleController {

    private final RoleService roleService;
    private final AdminProperties properties;
    private final ProductService productService;

    @Autowired
    public RoleController(RoleService roleService, AdminProperties properties, ProductService productService) {
        this.roleService = roleService;
        this.properties = properties;
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> result = roleService.listAll().stream()
                .map(this::toView)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Role> create(@RequestBody SaveRoleRequest request, HttpServletRequest http) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw com.iot.admin.common.AppException.badRequest("role code is required");
        }
        enforceProductCap(request, http);
        return ApiResponse.success(roleService.create(request.getCode(), request.getName(),
                request.getMenuIds(), request.getDataScopes()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Role> update(@PathVariable String id, @RequestBody SaveRoleRequest request,
                                    HttpServletRequest http) {
        enforceProductCap(request, http);
        return ApiResponse.success(roleService.update(id, request.getName(),
                request.getMenuIds(), request.getDataScopes(), request.getStatus()));
    }

    /** 机构管理员不能勾选机构产品范围外的菜单，也不能配置全域数据权限。 */
    private void enforceProductCap(SaveRoleRequest request, HttpServletRequest http) {
        if (Boolean.TRUE.equals(http.getAttribute("superAdmin"))) {
            return;
        }
        String orgCode = (String) http.getAttribute("org");
        Set<String> cap = productService.allowedMenuIdsForOrg(orgCode);
        if (request.getMenuIds() != null) {
            List<String> illegal = request.getMenuIds().stream()
                    .filter(id -> !cap.contains(id)).collect(Collectors.toList());
            if (!illegal.isEmpty()) {
                throw com.iot.admin.common.AppException.forbidden("菜单超出机构产品范围: " + illegal);
            }
        }
        // 机构管理员不具备配置全域数据权限的能力，统一收敛到本机构及子机构
        Map<String, String> scopes = new HashMap<String, String>();
        for (AdminProperties.Entity entity : properties.getEntities()) {
            scopes.put(entity.getCode(), RoleService.SCOPE_ORG_AND_CHILDREN);
        }
        request.setDataScopes(scopes);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ApiResponse.success(null);
    }

    /** 数据权限可配置的业务实体注册表（前端下拉数据源）。 */
    @GetMapping("/entities")
    public ApiResponse<List<AdminProperties.Entity>> entities() {
        return ApiResponse.success(properties.getEntities());
    }

    private Map<String, Object> toView(Role role) {
        Map<String, Object> view = new java.util.LinkedHashMap<String, Object>();
        view.put("id", role.getId());
        view.put("code", role.getCode());
        view.put("name", role.getName());
        view.put("menuIds", role.getMenuIds());
        view.put("dataScopes", role.getDataScopes());
        view.put("builtIn", role.isBuiltIn());
        view.put("status", role.getStatus());
        return view;
    }

    public static class SaveRoleRequest {
        /** 创建必填；更新时忽略（角色编码不可变） */
        private String code;
        private String name;
        private List<String> menuIds;
        private Map<String, String> dataScopes;
        private String status;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getMenuIds() { return menuIds; }
        public void setMenuIds(List<String> menuIds) { this.menuIds = menuIds; }
        public Map<String, String> getDataScopes() { return dataScopes; }
        public void setDataScopes(Map<String, String> dataScopes) { this.dataScopes = dataScopes; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
