package com.iot.admin.org;

import com.iot.admin.common.ApiResponse;
import com.iot.admin.common.AppException;
import com.iot.admin.internal.AuthInternalClient;
import com.iot.admin.security.RequireRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 机构管理：平台超管维护全部机构；机构管理员仅查看/编辑所属机构及其下级。 */
@RestController
@RequestMapping("/api/admin/orgs")
@RequireRoles({"SUPER_ADMIN", "ORG_ADMIN"})
public class OrgController {

    private final OrgService orgService;
    private final OrgRepository orgRepository;
    private final AuthInternalClient authClient;

    @Autowired
    public OrgController(OrgService orgService, OrgRepository orgRepository, AuthInternalClient authClient) {
        this.orgService = orgService;
        this.orgRepository = orgRepository;
        this.authClient = authClient;
    }

    /** 平台超管返回 null（全部机构）；机构管理员返回所属机构编码。 */
    private String orgScope(HttpServletRequest http) {
        if (Boolean.TRUE.equals(http.getAttribute("superAdmin"))) {
            return null;
        }
        return (String) http.getAttribute("org");
    }

    private boolean inScope(String code, String scope) {
        return code != null && scope != null && code.startsWith(scope);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterTree(List<Map<String, Object>> nodes, String scope) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> node : nodes) {
            String code = (String) node.get("code");
            List<Map<String, Object>> children = filterTree(
                    (List<Map<String, Object>>) node.get("children"), scope);
            if (inScope(code, scope) || !children.isEmpty()) {
                Map<String, Object> copy = new HashMap<String, Object>(node);
                copy.put("children", children);
                result.add(copy);
            }
        }
        return result;
    }

    @GetMapping
    public ApiResponse<List<Org>> list(HttpServletRequest http) {
        String scope = orgScope(http);
        List<Org> orgs = orgService.listAll();
        if (scope != null) {
            orgs.removeIf((org) -> !inScope(org.getCode(), scope));
        }
        return ApiResponse.success(orgs);
    }

    @GetMapping("/tree")
    public ApiResponse<List<Map<String, Object>>> tree(HttpServletRequest http) {
        String scope = orgScope(http);
        List<Map<String, Object>> nodes = orgService.tree();
        return ApiResponse.success(scope == null ? nodes : filterTree(nodes, scope));
    }

    @PostMapping
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Org> create(@RequestBody CreateOrgRequest request) {
        return ApiResponse.success(orgService.create(request.getCode(), request.getName(),
                request.getLeader(), request.getPhone(), request.getSort()));
    }

    /**
     * 一级机构 + 机构管理员：通过 auth-service 内部 API 用指定账号名和初始密码创建用户，
     * 绑定机构并授予 ORG_ADMIN 角色；机构产品权限随后由 sysadmin 分配。
     */
    @PostMapping("/create-with-admin")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Org> createWithAdmin(@RequestBody CreateOrgWithAdminRequest request) {
        Org org = orgService.create(request.getCode(), request.getName(), request.getLeader(),
                request.getPhone(), request.getSort());
        if (request.getAdminUsername() == null || request.getAdminUsername().trim().isEmpty()
                || request.getAdminPassword() == null || request.getAdminPassword().length() < 8) {
            throw com.iot.admin.common.AppException.badRequest("admin username required and password must be at least 8 characters");
        }
        List<String> roles = new ArrayList<String>();
        roles.add("ORG_ADMIN");
        AuthInternalClient.UserSummary admin = authClient.createUser(
                request.getAdminUsername().trim(), request.getAdminPassword(), roles);
        authClient.syncIdentity(admin.getId(), org.getId(), org.getCode(), new HashMap<String, String>());
        return ApiResponse.success(org);
    }

    @PutMapping("/{id}")
    public ApiResponse<Org> update(@PathVariable String id, @RequestBody UpdateOrgRequest request,
                                   HttpServletRequest http) {
        String scope = orgScope(http);
        Org target = orgRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("org not found: " + id));
        if (scope != null && !inScope(target.getCode(), scope)) {
            throw AppException.forbidden("机构超出当前账号管理范围");
        }
        return ApiResponse.success(orgService.update(id, request.getName(),
                request.getLeader(), request.getPhone(), request.getSort(), request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable String id) {
        orgService.delete(id, authClient::hasUsersUnderOrg);
        return ApiResponse.success(null);
    }

    public static class CreateOrgRequest {
        @NotBlank
        private String code;
        @NotBlank
        private String name;
        private String leader;
        private String phone;
        private String sort;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLeader() { return leader; }
        public void setLeader(String leader) { this.leader = leader; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
    }

    public static class CreateOrgWithAdminRequest extends CreateOrgRequest {
        private String adminUsername;
        private String adminPassword;
        public String getAdminUsername() { return adminUsername; }
        public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    }

    public static class UpdateOrgRequest {
        private String name;
        private String leader;
        private String phone;
        private String sort;
        private String status;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLeader() { return leader; }
        public void setLeader(String leader) { this.leader = leader; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
