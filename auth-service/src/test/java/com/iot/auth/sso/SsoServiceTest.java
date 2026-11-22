package com.iot.auth.sso;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SsoServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthProperties properties;
    private SsoService ssoService;

    private static final String WEB_SECRET = "web-secret-value";
    private static final String WEB_REDIRECT = "http://localhost:5173/sso/callback";

    @BeforeEach
    public void setUp() {
        properties = new AuthProperties();
        properties.setRedisPrefix("auth:");
        AuthProperties.Client client = new AuthProperties.Client();
        client.setId("web");
        client.setSecret(WEB_SECRET);
        client.setRedirectUris(Arrays.asList(WEB_REDIRECT));
        properties.setClients(Arrays.asList(client));
        when(redis.opsForValue()).thenReturn(valueOperations);
        ssoService = new SsoService(properties, redis);
    }

    @Test
    public void issueTicketStoresClientRedirectAndUser() {
        String ticket = ssoService.issueTicket("web", "http://localhost:5173/sso/callback", "u1");

        verify(valueOperations).set(eq("auth:ticket:" + ticket),
                eq("web|http://localhost:5173/sso/callback|u1"),
                anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void ticketRejectsUnregisteredRedirect() {
        try {
            ssoService.issueTicket("web", "https://evil.example.com/callback", "u1");
            fail("unregistered redirect should be rejected");
        } catch (AppException e) {
            assertEquals(400, e.getResultCode().getCode());
        }
    }

    @Test
    public void consumeTicketReturnsUserAndDeletesTicket() {
        when(valueOperations.get("auth:ticket:t1")).thenReturn("web|" + WEB_REDIRECT + "|u2");

        assertEquals("u2", ssoService.consumeTicket("t1", "web", WEB_SECRET, WEB_REDIRECT));
        verify(redis).delete("auth:ticket:t1");
    }

    @Test
    public void consumeTicketRejectsWrongSecret() {
        try {
            ssoService.consumeTicket("t3", "web", "wrong-secret", WEB_REDIRECT);
            fail("wrong client secret should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void consumeTicketRejectsMissingSecret() {
        try {
            ssoService.consumeTicket("t4", "web", null, WEB_REDIRECT);
            fail("missing client secret should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void consumeTicketRejectsRedirectMismatch() {
        when(valueOperations.get("auth:ticket:t2")).thenReturn("web|" + WEB_REDIRECT + "|u2");
        try {
            ssoService.consumeTicket("t2", "web", WEB_SECRET, "https://evil.example.com/callback");
            fail("redirect mismatch should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void consumeTicketRejectsUnknownClient() {
        try {
            ssoService.consumeTicket("t5", "app", "any", WEB_REDIRECT);
            fail("unknown client should be rejected");
        } catch (AppException e) {
            assertEquals(400, e.getResultCode().getCode());
        }
    }

    @Test
    public void missingTicketIsRejected() {
        when(valueOperations.get("auth:ticket:missing")).thenReturn(null);
        try {
            ssoService.consumeTicket("missing", "web", WEB_SECRET, WEB_REDIRECT);
            fail("missing ticket should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void clientWithoutSecretAllowsAnySecret() {
        AuthProperties.Client openClient = new AuthProperties.Client();
        openClient.setId("app");
        openClient.setRedirectUris(Arrays.asList("myapp://sso/callback"));
        properties.setClients(Arrays.asList(properties.getClients().get(0), openClient));
        when(valueOperations.get("auth:ticket:t6")).thenReturn("app|myapp://sso/callback|u7");

        assertEquals("u7", ssoService.consumeTicket("t6", "app", null, "myapp://sso/callback"));
    }
}
