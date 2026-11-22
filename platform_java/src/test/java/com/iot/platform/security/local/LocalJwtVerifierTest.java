package com.iot.platform.security.local;

import com.iot.platform.security.AuthServiceClient;
import com.iot.platform.security.AuthServiceException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class LocalJwtVerifierTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private JwksKeyResolver resolver;
    private LocalJwtVerifier verifier;
    private KeyPair keyPair;

    @Before
    public void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        restTemplate = new org.springframework.web.client.RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        resolver = new JwksKeyResolver(restTemplate, "http://auth:8083", "", 3600_000L, "", "k1");
        verifier = new LocalJwtVerifier(resolver, "iot-auth", "iot-platform", "k1");
    }

    private String jwksJson(String kid) {
        java.security.interfaces.RSAPublicKey pub = (java.security.interfaces.RSAPublicKey) keyPair.getPublic();
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + kid + "\",\"use\":\"sig\",\"alg\":\"RS256\","
                + "\"n\":\"" + enc.encodeToString(pub.getModulus().toByteArray()) + "\","
                + "\"e\":\"" + enc.encodeToString(pub.getPublicExponent().toByteArray()) + "\"}]}";
    }

    private String issueToken(String kid) {
        return Jwts.builder()
                .setSubject("u1")
                .claim("client", "web")
                .claim("roles", java.util.Arrays.asList("ADMIN"))
                .claim("ver", 3)
                .claim("type", "access")
                .setId("jti-1")
                .setHeaderParam("kid", kid)
                .setIssuer("iot-auth")
                .setAudience("iot-platform")
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact();
    }

    @Test
    public void verifiesTokenFromJwks() {
        server.expect(requestTo("http://auth:8083/.well-known/jwks.json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jwksJson("k1"), MediaType.APPLICATION_JSON));

        // 首次 isReady 同步拉取 JWKS 并缓存
        assertTrue(verifier.isReady());
        assertTrue(verifier.isReady());

        AuthServiceClient.TokenVerification verification = verifier.verify(issueToken("k1"));

        assertEquals("u1", verification.getSubject());
        assertEquals("web", verification.getClientId());
        assertEquals("jti-1", verification.getJti());
        assertEquals(3L, verification.getTokenVersion());
        assertTrue(verification.getRoles().contains("ADMIN"));
        server.verify();
    }

    @Test
    public void rejectsWrongSignature() throws Exception {
        server.expect(requestTo("http://auth:8083/.well-known/jwks.json"))
                .andRespond(withSuccess(jwksJson("k1"), MediaType.APPLICATION_JSON));
        verifier.isReady();

        // 用另一把密钥签发
        KeyPair other = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String forged = Jwts.builder()
                .setSubject("hacker")
                .setIssuer("iot-auth")
                .setAudience("iot-platform")
                .claim("type", "access")
                .signWith(SignatureAlgorithm.RS256, other.getPrivate())
                .compact();

        try {
            verifier.verify(forged);
            fail("forged token should be rejected");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void rejectsUnknownKidAfterFailedRefresh() {
        server.expect(requestTo("http://auth:8083/.well-known/jwks.json"))
                .andRespond(withSuccess(jwksJson("k1"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://auth:8083/.well-known/jwks.json"))
                .andRespond(withServerError());
        verifier.isReady();

        try {
            verifier.verify(issueToken("unknown-kid"));
            fail("unknown kid should be rejected");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
        }
    }
}
