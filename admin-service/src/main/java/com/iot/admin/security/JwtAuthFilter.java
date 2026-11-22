package com.iot.admin.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.admin.common.ApiResponse;
import com.iot.admin.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端鉴权过滤器（仅 /api/**）：Bearer 令牌本地验签（JWKS 公钥）+ 撤销检查；
 * 公钥未就绪时回退远程 /api/auth/verify（fail-closed，保持与既有体系一致）。
 * 身份写入 request 属性：subject/clientId/roles/tokenVersion/org/dataScopes/superAdmin。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final AdminAuthVerifier verifier;
    private final RevocationChecker revocationChecker;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String verifyUrl;

    public JwtAuthFilter(AdminAuthVerifier verifier, RevocationChecker revocationChecker,
                         RestTemplateBuilder builder,
                         @Value("${admin.auth.base-url:http://localhost:8083}") String baseUrl,
                         @Value("${admin.auth.verify-path:/api/auth/verify}") String verifyPath) {
        this.verifier = verifier;
        this.revocationChecker = revocationChecker;
        this.restTemplate = com.iot.admin.common.RestTemplates.build(builder, 2000, 2000);
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.verifyUrl = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + verifyPath;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "missing bearer token");
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        try {
            AdminAuthentication auth;
            if (verifier.isReady()) {
                auth = verifier.verify(token);
                revocationChecker.check(auth);
            } else {
                auth = remoteVerify(token);
            }
            request.setAttribute("subject", auth.getSubject());
            request.setAttribute("clientId", auth.getClientId());
            request.setAttribute("roles", auth.getRoles());
            request.setAttribute("tokenVersion", auth.getTokenVersion());
            request.setAttribute("org", auth.getOrgCode());
            request.setAttribute("dataScopes", auth.getDataScopes());
            request.setAttribute("superAdmin", auth.isSuperAdmin());
        } catch (com.iot.admin.common.AppException e) {
            writeError(response, e.getResultCode().getCode(), e.getMessage());
            return;
        } catch (Exception e) {
            log.warn("Unexpected failure verifying admin token", e);
            writeError(response, 503, "auth service unavailable");
            return;
        }
        chain.doFilter(request, response);
    }

    /** JWKS 未就绪时回退：远程 verify（auth-service 已做全量校验）。 */
    private AdminAuthentication remoteVerify(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            ResponseEntity<String> response = restTemplate.exchange(verifyUrl, HttpMethod.POST,
                    new HttpEntity<String>("{}", headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (response.getStatusCode().value() != 200 || data.isMissingNode()) {
                throw com.iot.admin.common.AppException.unauthorized("invalid or expired token");
            }
            List<String> roles = new ArrayList<String>();
            JsonNode rolesNode = data.path("roles");
            if (rolesNode.isArray()) {
                for (JsonNode role : rolesNode) {
                    roles.add(role.asText());
                }
            }
            Map<String, String> ds = new HashMap<String, String>();
            JsonNode dsNode = data.path("dataScopes");
            if (dsNode.isObject()) {
                for (Map.Entry<String, JsonNode> entry : iterableOf(dsNode)) {
                    ds.put(entry.getKey(), entry.getValue().asText());
                }
            }
            return new AdminAuthentication(data.path("subject").asText(null),
                    data.path("clientId").asText(null), roles,
                    data.path("tokenVersion").asLong(0L), data.path("jti").asText(null),
                    data.path("org").asText(null), ds);
        } catch (com.iot.admin.common.AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Remote verify failed: {}", e.getMessage());
            throw com.iot.admin.common.AppException.unauthorized("auth service unavailable");
        }
    }

    private List<Map.Entry<String, JsonNode>> iterableOf(JsonNode node) {
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<Map.Entry<String, JsonNode>>();
        java.util.Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            entries.add(it.next());
        }
        return entries;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(status == 503 ? ResultCode.ERROR.getCode() : status, message)));
    }
}
