package com.iot.admin.internal;

import com.iot.admin.common.AppException;
import com.iot.api.auth.ApiResponse;
import com.iot.api.auth.AuthInternalFeignClient;
import com.iot.api.auth.UserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * auth-service 内部 API 封装（/internal/users/**，Feign + HMAC 签名）。
 * 保留原有公开方法签名与 UserSummary 类型，调用方无需改动。
 */
@Service
public class AuthInternalClient {

    private static final Logger log = LoggerFactory.getLogger(AuthInternalClient.class);

    private final AuthInternalFeignClient feignClient;

    @Autowired
    public AuthInternalClient(AuthInternalFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    /** 用户摘要（兼容历史静态内部类引用；字段与共享契约一致）。 */
    public static class UserSummary {
        private String id;
        private String username;
        private List<String> roles;
        private String status;
        private long tokenVersion;
        private String orgId;
        private String orgCode;
        private Map<String, String> dataScopes = new HashMap<String, String>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getTokenVersion() { return tokenVersion; }
        public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
        public String getOrgId() { return orgId; }
        public void setOrgId(String orgId) { this.orgId = orgId; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public Map<String, String> getDataScopes() { return dataScopes; }
        public void setDataScopes(Map<String, String> dataScopes) { this.dataScopes = dataScopes; }
    }

    public List<UserSummary> listUsers() {
        List<UserSummary> result = new ArrayList<UserSummary>();
        for (com.iot.api.auth.UserSummary shared : unwrap(feignClient.listUsers(), "list users")) {
            result.add(toLegacy(shared));
        }
        return result;
    }

    public UserSummary createUser(String username, String password, List<String> roles) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", username);
        body.put("password", password);
        body.put("roles", roles == null ? new ArrayList<String>() : roles);
        return toLegacy(unwrap(feignClient.createUser(body), "create user"));
    }

    public UserSummary updateUser(String id, String username, List<String> roles, String status) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", username);
        body.put("roles", roles);
        body.put("status", status);
        return toLegacy(unwrap(feignClient.updateUser(id, body), "update user"));
    }

    public UserSummary resetPassword(String id, String password) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("password", password);
        return toLegacy(unwrap(feignClient.resetPassword(id, body), "reset password"));
    }

    public UserSummary syncIdentity(String id, String orgId, String orgCode,
                                    Map<String, String> dataScopes) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("orgId", orgId);
        body.put("orgCode", orgCode);
        body.put("dataScopes", dataScopes == null ? new HashMap<String, String>() : dataScopes);
        return toLegacy(unwrap(feignClient.syncIdentity(id, body), "sync identity"));
    }

    /** 机构删除守护：检查该机构（含子机构前缀）下是否仍有用户。 */
    public boolean hasUsersUnderOrg(String orgCode) {
        for (UserSummary user : listUsers()) {
            String userOrg = user.getOrgCode();
            if (userOrg != null && (userOrg.equals(orgCode) || userOrg.startsWith(orgCode))) {
                return true;
            }
        }
        return false;
    }

    private <T> T unwrap(ApiResponse<T> response, String action) {
        if (response == null || response.getCode() != 200) {
            String message = response == null ? "empty response" : response.getMessage();
            log.error("Internal call to auth-service failed: {} - {}", action, message);
            throw AppException.badRequest("auth service call failed: " + action + " - " + message);
        }
        return response.getData();
    }

    private static UserSummary toLegacy(com.iot.api.auth.UserSummary shared) {
        if (shared == null) return null;
        UserSummary u = new UserSummary();
        u.setId(shared.getId());
        u.setUsername(shared.getUsername());
        u.setRoles(shared.getRoles());
        u.setStatus(shared.getStatus());
        u.setTokenVersion(shared.getTokenVersion());
        u.setOrgId(shared.getOrgId());
        u.setOrgCode(shared.getOrgCode());
        if (shared.getDataScopes() != null) u.setDataScopes(shared.getDataScopes());
        return u;
    }
}
