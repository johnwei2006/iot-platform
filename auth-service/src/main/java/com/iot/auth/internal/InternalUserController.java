package com.iot.auth.internal;

import com.iot.api.auth.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内部用户管理 API（/internal/**，经 InternalAuthFilter HMAC 鉴权），
 * 仅供 admin-service 调用，不对外暴露。
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final InternalUserService userService;

    @Autowired
    public InternalUserController(InternalUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<InternalUserService.UserSummary>> list() {
        return ApiResponse.success(userService.list());
    }

    @PostMapping
    public ApiResponse<InternalUserService.UserSummary> create(@RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.create(request.getUsername(),
                request.getPassword(), request.getRoles()));
    }

    @PutMapping("/{id}")
    public ApiResponse<InternalUserService.UserSummary> update(@PathVariable String id,
                                                               @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.update(id, request.getUsername(),
                request.getRoles(), request.getStatus()));
    }

    /** 重置密码并递增 tokenVersion（全端强制下线）。 */
    @PostMapping("/{id}/password")
    public ApiResponse<InternalUserService.UserSummary> resetPassword(@PathVariable String id,
                                                                      @RequestBody Map<String, String> body) {
        return ApiResponse.success(userService.resetPassword(id, body.get("password")));
    }

    /** 同步机构与合并后的数据权限。 */
    @PostMapping("/{id}/identity")
    public ApiResponse<InternalUserService.UserSummary> syncIdentity(@PathVariable String id,
                                                                     @RequestBody IdentityRequest request) {
        return ApiResponse.success(userService.syncIdentity(id, request.getOrgId(),
                request.getOrgCode(), request.getDataScopes()));
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
        private List<String> roles;
        private String status;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class IdentityRequest {
        private String orgId;
        private String orgCode;
        private Map<String, String> dataScopes;
        public String getOrgId() { return orgId; }
        public void setOrgId(String orgId) { this.orgId = orgId; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public Map<String, String> getDataScopes() { return dataScopes; }
        public void setDataScopes(Map<String, String> dataScopes) { this.dataScopes = dataScopes; }
    }
}
