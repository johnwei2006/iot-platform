package com.iot.admin.org;

import com.iot.admin.common.ApiResponse;
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

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** 机构管理（仅 SUPER_ADMIN）。 */
@RestController
@RequestMapping("/api/admin/orgs")
@RequireRoles({"SUPER_ADMIN"})
public class OrgController {

    private final OrgService orgService;
    private final AuthInternalClient authClient;

    @Autowired
    public OrgController(OrgService orgService, AuthInternalClient authClient) {
        this.orgService = orgService;
        this.authClient = authClient;
    }

    @GetMapping
    public ApiResponse<List<Org>> list() {
        return ApiResponse.success(orgService.listAll());
    }

    @GetMapping("/tree")
    public ApiResponse<List<Map<String, Object>>> tree() {
        return ApiResponse.success(orgService.tree());
    }

    @PostMapping
    public ApiResponse<Org> create(@RequestBody CreateOrgRequest request) {
        return ApiResponse.success(orgService.create(request.getCode(), request.getName(),
                request.getLeader(), request.getPhone(), request.getSort()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Org> update(@PathVariable String id, @RequestBody UpdateOrgRequest request) {
        return ApiResponse.success(orgService.update(id, request.getName(),
                request.getLeader(), request.getPhone(), request.getSort(), request.getStatus()));
    }

    @DeleteMapping("/{id}")
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
