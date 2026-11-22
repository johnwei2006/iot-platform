package com.iot.admin.me;

import com.iot.admin.common.ApiResponse;
import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuService;
import com.iot.admin.product.ProductPermissionResolver;
import com.iot.admin.org.Org;
import com.iot.admin.org.OrgRepository;
import com.iot.admin.role.Role;
import com.iot.admin.role.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前管理员上下文（动态菜单/按钮权限/数据权限）：
 * JwtAuthFilter 已验签并写入身份属性；SUPER_ADMIN 拥有全部菜单与权限点。
 */
@RestController
@RequestMapping("/api/admin")
public class MeController {

    private final MenuService menuService;
    private final RoleService roleService;
    private final OrgRepository orgRepository;
    private final ProductPermissionResolver permissionResolver;

    @Autowired
    public MeController(MenuService menuService, RoleService roleService,
                        OrgRepository orgRepository, ProductPermissionResolver permissionResolver) {
        this.menuService = menuService;
        this.roleService = roleService;
        this.orgRepository = orgRepository;
        this.permissionResolver = permissionResolver;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        String subject = (String) request.getAttribute("subject");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute("roles");
        String orgCode = (String) request.getAttribute("org");
        @SuppressWarnings("unchecked")
        Map<String, String> dataScopes = (Map<String, String>) request.getAttribute("dataScopes");
        boolean superAdmin = Boolean.TRUE.equals(request.getAttribute("superAdmin"));

        List<String> roleMenuIds = new ArrayList<String>();
        if (!superAdmin) {
            List<Role> userRoles = roleService.findByCodes(roles == null
                    ? new ArrayList<String>() : roles);
            for (Role role : userRoles) {
                roleMenuIds.addAll(role.getMenuIds());
            }
        }
        ProductPermissionResolver.Resolved resolved = permissionResolver.resolve(orgCode, superAdmin, roleMenuIds);
        List<String> menuIds = menuIdsOf(resolved.getMenus());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("user", userInfo(subject, roles, orgCode, orgNameOf(orgCode), superAdmin));
        result.put("menus", superAdmin ? menuService.tree() : menuService.treeOf(menuIds));
        result.put("perms", resolved.getPerms());
        result.put("products", resolved.getProducts());
        result.put("dataScopes", dataScopes == null
                ? new LinkedHashMap<String, String>() : dataScopes);
        return ApiResponse.success(result);
    }

    private List<String> menuIdsOf(List<Menu> menus) {
        List<String> ids = new ArrayList<String>();
        for (Menu menu : menus) {
            ids.add(menu.getId());
        }
        return ids;
    }

    private Map<String, Object> userInfo(String subject, List<String> roles,
                                         String orgCode, String orgName, boolean superAdmin) {
        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("id", subject);
        user.put("username", subject);
        user.put("roles", roles == null ? new ArrayList<String>() : roles);
        user.put("orgCode", orgCode);
        user.put("orgName", orgName);
        user.put("superAdmin", superAdmin);
        return user;
    }

    /** 机构编码 → 名称；未匹配时回退显示编码本身。 */
    private String orgNameOf(String orgCode) {
        if (orgCode == null || orgCode.isEmpty()) {
            return "";
        }
        return orgRepository.findByCode(orgCode).map(Org::getName).orElse(orgCode);
    }
}
