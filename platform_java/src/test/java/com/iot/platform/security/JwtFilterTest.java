package com.iot.platform.security;

import com.iot.platform.security.local.LocalJwtVerifier;
import com.iot.platform.security.local.RevocationChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class JwtFilterTest {

    private AuthServiceClient client;
    private LocalJwtVerifier localVerifier;
    private RevocationChecker revocationChecker;
    private JwtFilter filter;

    @BeforeEach
    public void setUp() {
        client = mock(AuthServiceClient.class);
        localVerifier = mock(LocalJwtVerifier.class);
        revocationChecker = mock(RevocationChecker.class);
        // 默认公钥未就绪 → 走远程回退路径（保持原行为）
        when(localVerifier.isReady()).thenReturn(false);
        filter = new JwtFilter(client, localVerifier, revocationChecker);
    }

    @Test
    public void whitelistedPathSkipsVerification() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/api/auth/login", null), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        verifyNoInteractions(client);
    }

    @Test
    public void missingAuthorizationHeaderReturns401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/devices", null), response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
        verifyNoInteractions(client);
    }

    @Test
    public void validTokenSetsIdentityAttributesAndContinues() throws Exception {
        when(client.verify("good-token")).thenReturn(
                new AuthServiceClient.TokenVerification("u1", "web", Arrays.asList("ADMIN"), 3L));
        MockHttpServletRequest request = request("/api/devices", "Bearer good-token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        assertEquals("u1", request.getAttribute("subject"));
        assertEquals("web", request.getAttribute("clientId"));
        assertEquals(Arrays.asList("ADMIN"), request.getAttribute("roles"));
        assertEquals(3L, request.getAttribute("tokenVersion"));
    }

    @Test
    public void invalidTokenReturns401WithoutContinuing() throws Exception {
        when(client.verify("bad")).thenThrow(AuthServiceException.unauthorized("token revoked"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/api/devices", "Bearer bad"), response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("token revoked"));
        assertEquals(null, chain.getRequest());
    }

    @Test
    public void unavailableAuthServiceReturns503() throws Exception {
        when(client.verify("token")).thenThrow(AuthServiceException.unavailable("auth service down"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/devices", "Bearer token"), response, new MockFilterChain());

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("auth service down"));
    }

    @Test
    public void localVerifyUsedWhenKeyReady() throws Exception {
        when(localVerifier.isReady()).thenReturn(true);
        when(localVerifier.verify("local-token")).thenReturn(
                new AuthServiceClient.TokenVerification("u2", "mini", Arrays.asList("ADMIN"), 1L, "jti-1"));
        MockHttpServletRequest request = request("/api/devices", "Bearer local-token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        assertEquals("u2", request.getAttribute("subject"));
        verifyNoInteractions(client);
        verify(revocationChecker).check(org.mockito.ArgumentMatchers.any(AuthServiceClient.TokenVerification.class));
    }

    private MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
