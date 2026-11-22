package com.iot.admin.user;

import com.iot.admin.common.ApiResponse;
import com.iot.admin.common.AppException;
import com.iot.admin.internal.AuthInternalClient;
import com.iot.admin.internal.AuthInternalClient.UserSummary;
import com.iot.admin.org.Org;
import com.iot.admin.org.OrgRepository;
import com.iot.admin.role.Role;
import com.iot.admin.role.RoleService;
import com.iot.admin.security.RequireRoles;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户管理：平台超管管理全部账号；机构管理员仅管理本机构及下级用户，
 * 且不能授予 SUPER_ADMIN 或跨机构分配。凭证由 auth-service 唯一持有，
 * admin-service 经 HMAC 内部 API 代理增改，并负责机构归属与角色分配。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRoles({"SUPER_ADMIN", "ORG_ADMIN"})
public class AdminUserController {

    private final AuthInternalClient authClient;
    private final RoleService roleService;
    private final OrgRepository orgRepository;

    @Autowired
    public AdminUserController(AuthInternalClient authClient, RoleService roleService,
                               OrgRepository orgRepository) {
        this.authClient = authClient;
        this.roleService = roleService;
        this.orgRepository = orgRepository;
    }

    /** 平台超管返回 null；机构管理员返回所属机构编码。 */
    private String orgScope(HttpServletRequest http) {
        if (Boolean.TRUE.equals(http.getAttribute("superAdmin"))) {
            return null;
        }
        return (String) http.getAttribute("org");
    }

    private void ensureScope(UserSummary user, String scope) {
        if (scope != null && (user.getOrgCode() == null || !user.getOrgCode().startsWith(scope))) {
            throw AppException.forbidden("用户超出当前机构管理范围");
        }
    }

    private void ensureRolesAllowed(List<String> roles, String scope) {
        if (scope == null || roles == null) {
            return;
        }
        for (String role : roles) {
            if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
                throw AppException.forbidden("机构管理员不能授予平台超级管理员角色");
            }
        }
    }

    @GetMapping
    public ApiResponse<List<UserSummary>> list(HttpServletRequest http) {
        String scope = orgScope(http);
        List<UserSummary> users = authClient.listUsers();
        if (scope != null) {
            users.removeIf((user) -> user.getOrgCode() == null || !user.getOrgCode().startsWith(scope));
        }
        return ApiResponse.success(users);
    }

    /** 创建用户（凭证 + 初始角色）；机构随后可通过 assign 接口分配。 */
    @PostMapping
    public ApiResponse<UserSummary> create(@RequestBody CreateUserRequest request, HttpServletRequest http) {
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw AppException.badRequest("password must be at least 8 characters");
        }
        String scope = orgScope(http);
        List<String> roles = request.getRoles() == null ? new ArrayList<String>() : request.getRoles();
        ensureRolesAllowed(roles, scope);
        UserSummary user = authClient.createUser(request.getUsername(), request.getPassword(), roles);
        if (scope != null) {
            Org org = orgRepository.findByCode(scope)
                    .orElseThrow(() -> AppException.badRequest("org not found: " + scope));
            user = authClient.syncIdentity(user.getId(), org.getId(), org.getCode(),
                    roleService.mergeDataScopes(roleService.findByCodes(roles)));
        }
        return ApiResponse.success(user);
    }

    /** 更新用户名/启停。 */
    @PutMapping("/{id}")
    public ApiResponse<UserSummary> update(@PathVariable String id,
                                           @RequestBody UpdateUserRequest request,
                                           HttpServletRequest http) {
        ensureScope(findUser(id), orgScope(http));
        return ApiResponse.success(authClient.updateUser(id, request.getUsername(),
                null, request.getStatus()));
    }

    /** 重置密码（tokenVersion 递增，全端强制下线）。 */
    @PostMapping("/{id}/password")
    public ApiResponse<UserSummary> resetPassword(@PathVariable String id,
                                                  @RequestBody Map<String, String> body,
                                                  HttpServletRequest http) {
        String password = body.get("password");
        if (password == null || password.length() < 8) {
            throw AppException.badRequest("password must be at least 8 characters");
        }
        ensureScope(findUser(id), orgScope(http));
        return ApiResponse.success(authClient.resetPassword(id, password));
    }

    /**
     * 分配机构与/或角色：合并 dataScopes（每实体取最宽）并同步到 auth-service。
     * roles 与 orgId 至少提供一个。
     */
    @PostMapping("/{id}/assign")
    public ApiResponse<UserSummary> assign(@PathVariable String id,
                                           @RequestBody AssignRequest request,
                                           HttpServletRequest http) {
        if (request.getRoles() == null && request.getOrgId() == null) {
            throw AppException.badRequest("roles or orgId required");
        }
        String scope = orgScope(http);
        UserSummary current = findUser(id);
        ensureScope(current, scope);
        List<String> roles = request.getRoles() != null
                ? request.getRoles() : current.getRoles();
        ensureRolesAllowed(roles, scope);
        String orgId = request.getOrgId() != null ? request.getOrgId() : current.getOrgId();
        String orgCode = current.getOrgCode();
        if (request.getOrgId() != null) {
            Org org = orgRepository.findById(request.getOrgId())
                    .orElseThrow(() -> AppException.badRequest("org not found: " + request.getOrgId()));
            if (scope != null && !org.getCode().startsWith(scope)) {
                throw AppException.forbidden("不能将用户分配到当前机构之外");
            }
            orgCode = org.getCode();
        }
        // 先更新角色，再按最终角色合并数据权限并同步机构信息
        UserSummary updated = authClient.updateUser(id, null, roles, null);
        Map<String, String> merged = roleService.mergeDataScopes(
                roleService.findByCodes(roles));
        if (scope != null) {
            // 机构管理员不能通过角色携带的全域数据权限越权
            for (Map.Entry<String, String> entry : merged.entrySet()) {
                if (RoleService.SCOPE_ALL.equals(entry.getValue())) {
                    entry.setValue(RoleService.SCOPE_ORG_AND_CHILDREN);
                }
            }
        }
        return ApiResponse.success(authClient.syncIdentity(id, orgId, orgCode, merged));
    }

    private UserSummary findUser(String id) {
        for (UserSummary user : authClient.listUsers()) {
            if (id.equals(user.getId())) {
                return user;
            }
        }
        throw AppException.notFound("user not found: " + id);
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private List<String> roles;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public static class UpdateUserRequest {
        private String username;
        private String status;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AssignRequest {
        private List<String> roles;
        private String orgId;
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getOrgId() { return orgId; }
        public void setOrgId(String orgId) { this.orgId = orgId; }
    }
}
