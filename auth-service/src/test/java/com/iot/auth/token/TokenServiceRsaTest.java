package com.iot.auth.token;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TokenServiceRsaTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthProperties properties;
    private TokenService tokenService;
    private String publicJwkKid = "test-key-1";

    @Before
    public void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        byte[] pkcs8 = pair.getPrivate().getEncoded();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pkcs8)
                + "\n-----END PRIVATE KEY-----";

        properties = new AuthProperties();
        properties.setJwtPrivateKey(pem);
        properties.setJwtKeyId(publicJwkKid);
        properties.setJwtIssuer("iot-auth");
        properties.setJwtAudience("iot-platform");
        properties.setRedisPrefix("auth:");
        properties.validate();
        when(redis.opsForValue()).thenReturn(valueOperations);
        tokenService = new TokenService(properties, redis);
        tokenService.initCrypto();
    }

    @Test
    public void roundTripWithRsaAndIssuerAudience() {
        TokenClaims claims = new TokenClaims("u1", "web", Arrays.asList("ADMIN"),
                "jti-1", 2L, "dev-1");

        String token = tokenService.issueAccessToken(claims);
        TokenClaims parsed = tokenService.verifyAccessToken(token);

        assertEquals("u1", parsed.getSubject());
        assertEquals(2L, parsed.getTokenVersion());
        // kid 头
        String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertTrue(header.contains("\"kid\":\"test-key-1\""));
    }

    @Test
    public void publicJwkContainsModulusAndExponent() {
        Map<String, Object> jwk = tokenService.publicJwk();
        assertNotNull(jwk);
        assertEquals("RSA", jwk.get("kty"));
        assertEquals("test-key-1", jwk.get("kid"));
        assertEquals("RS256", jwk.get("alg"));
        assertNotNull(jwk.get("n"));
        assertNotNull(jwk.get("e"));
    }

    @Test
    public void hsmModeWithoutKeyReturnsNullJwk() {
        // HS256 兼容模式
        AuthProperties hmac = new AuthProperties();
        hmac.setJwtSecret("test-secret-at-least-32-characters-long-1234567890");
        hmac.setRedisPrefix("auth:");
        TokenService hmacService = new TokenService(hmac, redis);
        hmacService.initCrypto();
        assertNull(hmacService.publicJwk());
        // HS256 签发的 token 可正常往返
        TokenClaims claims = new TokenClaims("u2", "mini", Arrays.asList("TERMINAL"),
                "jti-2", 1L, "");
        assertEquals("u2", hmacService.verifyAccessToken(hmacService.issueAccessToken(claims))
                .getSubject());
    }

    @Test
    public void tamperedTokenIsRejected() {
        TokenClaims claims = new TokenClaims("u1", "web", Arrays.asList("ADMIN"),
                "jti-3", 1L, "");
        String token = tokenService.issueAccessToken(claims);
        String[] parts = token.split("\\.");
        // 篡改 payload
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin\"}".getBytes());
        try {
            tokenService.verifyAccessToken(String.join(".", parts));
            fail("tampered token should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void wrongIssuerOrAudienceIsRejected() throws Exception {
        // 用同一 RSA 密钥但错误 iss/aud 手工签发
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) privateKeyFromService();
        String wrong = Jwts.builder()
                .setSubject("u9")
                .setIssuer("other-issuer")
                .setAudience("iot-platform")
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
        try {
            tokenService.verifyAccessToken(wrong);
            fail("wrong issuer should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    private java.security.PrivateKey privateKeyFromService() throws Exception {
        // 借助 PKCS8 重新构造（测试辅助）
        java.security.KeyFactory factory = java.security.KeyFactory.getInstance("RSA");
        return factory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(
                Base64.getMimeDecoder().decode(properties.getJwtPrivateKey()
                        .replaceAll("-----BEGIN [A-Z ]+-----", "")
                        .replaceAll("-----END [A-Z ]+-----", "")
                        .replaceAll("\\s", ""))));
    }
}
