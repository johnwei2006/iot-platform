package com.iot.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.ApiResponse;
import com.iot.platform.security.local.LocalJwtVerifier;
import com.iot.platform.security.local.RevocationChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 统一鉴权过滤器：校验 Bearer 令牌，白名单路径跳过。
 *
 * <p>验签策略（阶段 3）：公钥就绪时走本地验签（RS256，{@link com.iot.platform.security.local.LocalJwtVerifier}），
 * 再由本地撤销层做黑名单/tokenVersion 检查——auth-service 故障不阻断业务；
 * 公钥未就绪时自动回退远程验签（{@link AuthServiceClient}，fail-closed）。
 * 校验通过后把身份声明写入 request 属性：subject、clientId、roles、tokenVersion。
 * 白名单：登录、健康检查、actuator、静态资源。
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final AuthServiceClient authServiceClient;
    private final LocalJwtVerifier localVerifier;
    private final RevocationChecker revocationChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> whiteList = Arrays.asList(
            "/api/auth/**", "/health", "/actuator/**", "/error", "/favicon.ico");

    public JwtFilter(AuthServiceClient authServiceClient,
                     LocalJwtVerifier localVerifier,
                     RevocationChecker revocationChecker) {
        this.authServiceClient = authServiceClient;
        this.localVerifier = localVerifier;
        this.revocationChecker = revocationChecker;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String p : whiteList) {
            if (matcher.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "missing Authorization header");
            return;
        }
        try {
            String token = header.substring("Bearer ".length()).trim();
            AuthServiceClient.TokenVerification verification;
            if (localVerifier.isReady()) {
                verification = localVerifier.verify(token);
                revocationChecker.check(verification);
            } else {
                verification = authServiceClient.verify(token);
            }
            request.setAttribute("subject", verification.getSubject());
            request.setAttribute("clientId", verification.getClientId());
            request.setAttribute("roles", verification.getRoles());
            request.setAttribute("tokenVersion", verification.getTokenVersion());
            request.setAttribute("org", verification.getOrgCode());
            request.setAttribute("dataScopes", verification.getDataScopes());
        } catch (AuthServiceException e) {
            writeError(response, e.getStatus(), e.getMessage());
            return;
        } catch (Exception e) {
            logger.warn("Unexpected failure verifying access token", e);
            writeError(response, 503, "auth service unavailable");
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> r = ApiResponse.fail(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
