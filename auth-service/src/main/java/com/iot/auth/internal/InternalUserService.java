package com.iot.auth.internal;

import com.iot.auth.audit.AuditService;
import com.iot.auth.common.AppException;
import com.iot.auth.identity.User;
import com.iot.auth.identity.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部用户管理（供 admin-service 经 HMAC 调用）：
 * 用户资料与机构/数据权限由 admin-service 作为权威源维护，
 * 本服务只负责凭证（BCrypt 密码）、tokenVersion 与最终落库。
 */
@Service
public class InternalUserService {

    private static final Logger log = LoggerFactory.getLogger(InternalUserService.class);

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public InternalUserService(UserRepository userRepository, StringRedisTemplate redis,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.redis = redis;
        this.auditService = auditService;
    }

    /** 全量用户列表（供管理页查询；内部接口，暂不分页，上限 1000）。 */
    public List<UserSummary> list() {
        List<UserSummary> result = new ArrayList<UserSummary>();
        for (User user : userRepository.findAll()) {
            result.add(toSummary(user));
        }
        return result.size() > 1000 ? result.subList(0, 1000) : result;
    }

    /** 创建用户（分配凭证），角色与机构信息随后由 identity 接口同步。 */
    public UserSummary create(String username, String password, List<String> roles) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw AppException.conflict("username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(roles == null ? new ArrayList<String>() : new ArrayList<String>(roles));
        user.setStatus("ENABLED");
        user.setTokenVersion(0L);
        user.setCreatedAt(new java.util.Date());
        User saved = userRepository.save(user);
        log.info("Internal user created: {} ({})", username, saved.getId());
        return toSummary(saved);
    }

    /** 更新用户名/角色/状态。 */
    public UserSummary update(String id, String username, List<String> roles, String status) {
        User user = requireUser(id);
        if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
            if (userRepository.findByUsername(username.trim()).isPresent()) {
                throw AppException.conflict("username already exists: " + username);
            }
            user.setUsername(username.trim());
        }
        if (roles != null) {
            user.setRoles(new ArrayList<String>(roles));
        }
        if (status != null && ("ENABLED".equals(status) || "DISABLED".equals(status))) {
            if ("DISABLED".equals(status) && user.isEnabled()) {
                bumpTokenVersion(user);
            }
            user.setStatus(status);
        }
        return toSummary(userRepository.save(user));
    }

    /** 重置密码：更新 BCrypt 哈希并递增 tokenVersion 强制全部会话下线。 */
    public UserSummary resetPassword(String id, String password) {
        User user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(password));
        bumpTokenVersion(user);
        User saved = userRepository.save(user);
        auditService.record(AuditService.REVOKE_ALL, user.getId(), user.getUsername(), "admin_service",
                null, "password reset via internal api");
        return toSummary(saved);
    }

    /** 同步机构与数据权限（角色分配后由 admin-service 计算合并结果）。 */
    public UserSummary syncIdentity(String id, String orgId, String orgCode,
                                    Map<String, String> dataScopes) {
        User user = requireUser(id);
        user.setOrgId(orgId);
        user.setOrgCode(orgCode);
        user.setDataScopes(dataScopes == null ? new HashMap<String, String>() : dataScopes);
        // 机构/权限变更需重新签发 token 才生效
        bumpTokenVersion(user);
        return toSummary(userRepository.save(user));
    }

    private void bumpTokenVersion(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1L);
        redis.delete("auth:userver:" + user.getId());
    }

    private User requireUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("user not found: " + id));
    }

    private UserSummary toSummary(User user) {
        UserSummary summary = new UserSummary();
        summary.setId(user.getId());
        summary.setUsername(user.getUsername());
        summary.setRoles(user.getRoles());
        summary.setStatus(user.getStatus());
        summary.setTokenVersion(user.getTokenVersion());
        summary.setOrgId(user.getOrgId());
        summary.setOrgCode(user.getOrgCode());
        summary.setDataScopes(user.getDataScopes());
        return summary;
    }

    public static class UserSummary {
        private String id;
        private String username;
        private List<String> roles;
        private String status;
        private long tokenVersion;
        private String orgId;
        private String orgCode;
        private Map<String, String> dataScopes;

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
}
