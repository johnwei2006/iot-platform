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
 * 用户管理（仅 SUPER_ADMIN）：凭证由 auth-service 唯一持有，
 * admin-service 经 HMAC 内部 API 代理增改，并负责机构归属与角色分配
 * ——分配时合并数据权限并同步到 auth-service（tokenVersion 递增强制重新签发）。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRoles({"SUPER_ADMIN"})
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

    @GetMapping
    public ApiResponse<List<UserSummary>> list() {
        return ApiResponse.success(authClient.listUsers());
    }

    /** 创建用户（凭证 + 初始角色）；机构随后可通过 assign 接口分配。 */
    @PostMapping
    public ApiResponse<UserSummary> create(@RequestBody CreateUserRequest request) {
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw AppException.badRequest("password must be at least 8 characters");
        }
        return ApiResponse.success(authClient.createUser(request.getUsername(),
                request.getPassword(), request.getRoles() == null
                        ? new ArrayList<String>() : request.getRoles()));
    }

    /** 更新用户名/启停。 */
    @PutMapping("/{id}")
    public ApiResponse<UserSummary> update(@PathVariable String id,
                                           @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(authClient.updateUser(id, request.getUsername(),
                null, request.getStatus()));
    }

    /** 重置密码（tokenVersion 递增，全端强制下线）。 */
    @PostMapping("/{id}/password")
    public ApiResponse<UserSummary> resetPassword(@PathVariable String id,
                                                  @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.length() < 8) {
            throw AppException.badRequest("password must be at least 8 characters");
        }
        return ApiResponse.success(authClient.resetPassword(id, password));
    }

    /**
     * 分配机构与/或角色：合并 dataScopes（每实体取最宽）并同步到 auth-service。
     * roles 与 orgId 至少提供一个。
     */
    @PostMapping("/{id}/assign")
    public ApiResponse<UserSummary> assign(@PathVariable String id,
                                           @RequestBody AssignRequest request) {
        if (request.getRoles() == null && request.getOrgId() == null) {
            throw AppException.badRequest("roles or orgId required");
        }
        UserSummary current = findUser(id);
        List<String> roles = request.getRoles() != null
                ? request.getRoles() : current.getRoles();
        String orgId = request.getOrgId() != null ? request.getOrgId() : current.getOrgId();
        String orgCode = current.getOrgCode();
        if (request.getOrgId() != null) {
            Org org = orgRepository.findById(request.getOrgId())
                    .orElseThrow(() -> AppException.badRequest("org not found: " + request.getOrgId()));
            orgCode = org.getCode();
        }
        // 先更新角色，再按最终角色合并数据权限并同步机构信息
        UserSummary updated = authClient.updateUser(id, null, roles, null);
        Map<String, String> merged = roleService.mergeDataScopes(
                roleService.findByCodes(roles));
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
