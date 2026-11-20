package com.iot.platform.security;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class AuthServiceClientTest {

    private MockRestServiceServer server;
    private AuthServiceClient client;

    @Before
    public void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new AuthServiceClient(restTemplate, "http://auth:8083/", "/api/auth/verify");
    }

    @Test
    public void verifyReturnsClaimsFromAuthService() {
        server.expect(requestTo("http://auth:8083/api/auth/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer good-token"))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"success\","
                        + "\"data\":{\"subject\":\"u1\",\"clientId\":\"web\","
                        + "\"roles\":[\"ADMIN\"],\"tokenVersion\":2},\"timestamp\":1}",
                        MediaType.APPLICATION_JSON));

        AuthServiceClient.TokenVerification result = client.verify("good-token");

        assertEquals("u1", result.getSubject());
        assertEquals("web", result.getClientId());
        assertEquals(Arrays.asList("ADMIN"), result.getRoles());
        assertEquals(2L, result.getTokenVersion());
        server.verify();
    }

    @Test
    public void verifyRejectsBusinessError() {
        server.expect(requestTo("http://auth:8083/api/auth/verify"))
                .andRespond(withSuccess("{\"code\":401,\"message\":\"token revoked\"}",
                        MediaType.APPLICATION_JSON));
        assertUnauthorized("token revoked");
        server.verify();
    }

    @Test
    public void verifyRejectsHttpUnauthorized() {
        server.expect(requestTo("http://auth:8083/api/auth/verify"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertUnauthorized("invalid access token");
        server.verify();
    }

    @Test
    public void verifyMapsServerErrorToUnavailable() {
        server.expect(requestTo("http://auth:8083/api/auth/verify"))
                .andRespond(withServerError());
        try {
            client.verify("token");
            fail("expected AuthServiceException");
        } catch (AuthServiceException e) {
            assertEquals(503, e.getStatus());
        }
        server.verify();
    }

    @Test
    public void verifyRejectsEmptyTokenWithoutCallingAuthService() {
        try {
            client.verify("  ");
            fail("expected AuthServiceException");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
        }
        server.verify();
    }

    @Test
    public void postForwardsBodyAndReturnsUpstreamResponse() {
        server.expect(requestTo("http://auth:8083/api/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().json("{\"username\":\"admin\"}"))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"accessToken\":\"jwt\"}}",
                        MediaType.APPLICATION_JSON));

        AuthServiceClient.ProxyResponse response =
                client.post("api/auth/login", "{\"username\":\"admin\"}");

        assertEquals(200, response.getStatus());
        assertEquals("{\"code\":200,\"data\":{\"accessToken\":\"jwt\"}}", response.getBody());
        server.verify();
    }

    @Test
    public void postPreservesUpstreamErrorStatusAndBody() {
        server.expect(requestTo("http://auth:8083/api/auth/login"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":401,\"message\":\"bad credentials\"}"));

        AuthServiceClient.ProxyResponse response = client.post("/api/auth/login", "{}");

        assertEquals(401, response.getStatus());
        assertEquals("{\"code\":401,\"message\":\"bad credentials\"}", response.getBody());
        server.verify();
    }

    private void assertUnauthorized(String message) {
        try {
            client.verify("token");
            fail("expected AuthServiceException");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
            assertEquals(message, e.getMessage());
        }
    }
}
