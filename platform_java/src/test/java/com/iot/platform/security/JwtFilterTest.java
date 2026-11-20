package com.iot.platform.security;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JwtFilterTest {

    private AuthServiceClient client;
    private JwtFilter filter;

    @Before
    public void setUp() {
        client = mock(AuthServiceClient.class);
        filter = new JwtFilter(client);
    }

    @Test
    public void whitelistedPathSkipsVerification() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/api/auth/login", null), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        verifyZeroInteractions(client);
    }

    @Test
    public void missingAuthorizationHeaderReturns401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/devices", null), response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
        verifyZeroInteractions(client);
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

    private MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
