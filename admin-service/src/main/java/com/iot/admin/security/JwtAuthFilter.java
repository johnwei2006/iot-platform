package com.iot.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.admin.common.ApiResponse;
import com.iot.admin.common.ResultCode;
import com.iot.api.auth.AuthDtos;
import com.iot.api.auth.AuthFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
    private final AuthFeignClient authFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter(AdminAuthVerifier verifier, RevocationChecker revocationChecker,
                         AuthFeignClient authFeignClient) {
        this.verifier = verifier;
        this.revocationChecker = revocationChecker;
        this.authFeignClient = authFeignClient;
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

    /** JWKS 未就绪时回退：通过 Feign 调用远程 verify（auth-service 已做全量校验）。 */
    private AdminAuthentication remoteVerify(String token) {
        try {
            com.iot.api.auth.ApiResponse<AuthDtos.VerifyResponse> response =
                    authFeignClient.verify("Bearer " + token, "{}");
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                throw com.iot.admin.common.AppException.unauthorized("invalid or expired token");
            }
            AuthDtos.VerifyResponse data = response.getData();
            List<String> roles = data.getRoles() == null ? new ArrayList<String>() : data.getRoles();
            Map<String, String> ds = data.getDataScopes() == null ? new HashMap<String, String>() : data.getDataScopes();
            return new AdminAuthentication(data.getSubject(), data.getClientId(), roles,
                    data.getTokenVersion(), data.getJti(), data.getOrg(), ds);
        } catch (com.iot.admin.common.AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Remote verify failed: {}", e.getMessage());
            throw com.iot.admin.common.AppException.unauthorized("auth service unavailable");
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(status == 503 ? ResultCode.ERROR.getCode() : status, message)));
    }
}
