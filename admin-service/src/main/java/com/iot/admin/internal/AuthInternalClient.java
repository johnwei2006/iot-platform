package com.iot.admin.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.admin.common.AppException;
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
 * auth-service 内部 API 封装（/internal/users/**，HMAC 鉴权）：
 * 用户列表/创建/更新/重置密码/身份（机构+数据权限）同步。
 */
@Service
public class AuthInternalClient {

    private static final Logger log = LoggerFactory.getLogger(AuthInternalClient.class);

    private final HmacClient hmacClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AuthInternalClient(HmacClient.Factory factory) {
        this.hmacClient = factory.create();
    }

    /** 用户摘要（与 auth-service InternalUserService.UserSummary 对齐）。 */
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
        JsonNode data = call("GET", "/internal/users", null);
        List<UserSummary> users = new ArrayList<UserSummary>();
        if (data.isArray()) {
            for (JsonNode node : data) {
                users.add(toSummary(node));
            }
        }
        return users;
    }

    public UserSummary createUser(String username, String password, List<String> roles) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", username);
        body.put("password", password);
        body.put("roles", roles == null ? new ArrayList<String>() : roles);
        return toSummary(call("POST", "/internal/users", toJson(body)));
    }

    public UserSummary updateUser(String id, String username, List<String> roles, String status) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", username);
        body.put("roles", roles);
        body.put("status", status);
        return toSummary(call("PUT", "/internal/users/" + id, toJson(body)));
    }

    public UserSummary resetPassword(String id, String password) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("password", password);
        return toSummary(call("POST", "/internal/users/" + id + "/password", toJson(body)));
    }

    public UserSummary syncIdentity(String id, String orgId, String orgCode,
                                    Map<String, String> dataScopes) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("orgId", orgId);
        body.put("orgCode", orgCode);
        body.put("dataScopes", dataScopes == null ? new HashMap<String, String>() : dataScopes);
        return toSummary(call("POST", "/internal/users/" + id + "/identity", toJson(body)));
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

    private UserSummary toSummary(JsonNode node) {
        UserSummary summary = new UserSummary();
        summary.setId(node.path("id").asText(null));
        summary.setUsername(node.path("username").asText(null));
        summary.setRoles(toStringList(node.path("roles")));
        summary.setStatus(node.path("status").asText(null));
        summary.setTokenVersion(node.path("tokenVersion").asLong(0L));
        summary.setOrgId(node.path("orgId").asText(null));
        summary.setOrgCode(node.path("orgCode").asText(null));
        JsonNode ds = node.path("dataScopes");
        if (ds.isObject()) {
            for (Map.Entry<String, JsonNode> entry : entriesOf(ds)) {
                summary.getDataScopes().put(entry.getKey(), entry.getValue().asText());
            }
        }
        return summary;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<String>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private List<Map.Entry<String, JsonNode>> entriesOf(JsonNode node) {
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<Map.Entry<String, JsonNode>>();
        java.util.Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            entries.add(it.next());
        }
        return entries;
    }

    /** 解析 ApiResponse 包裹：非 200 抛业务异常（透传 auth-service message）。 */
    private JsonNode call(String method, String path, String body) {
        String response;
        try {
            response = hmacClient.exchange(method, path, body);
        } catch (Exception e) {
            log.error("Internal call to auth-service failed: {} {}", method, path, e);
            throw AppException.badRequest("auth service call failed: " + path);
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                throw AppException.badRequest(root.path("message").asText("internal call rejected"));
            }
            return root.path("data");
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.badRequest("invalid response from auth service");
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("json serialization failed", e);
        }
    }
}
