package com.iot.auth.sso;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SsoServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthProperties properties;
    private SsoService ssoService;

    @Before
    public void setUp() {
        properties = new AuthProperties();
        properties.setRedisPrefix("auth:");
        AuthProperties.Client client = new AuthProperties.Client();
        client.setId("web");
        client.setRedirectUris(Arrays.asList("http://localhost:5173/sso/callback"));
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
        when(valueOperations.get("auth:ticket:t1")).thenReturn("web|http://localhost:5173/sso/callback|u2");

        assertEquals("u2", ssoService.consumeTicket("t1", "web"));
        verify(redis).delete("auth:ticket:t1");
    }

    @Test
    public void consumeTicketRejectsClientMismatch() {
        when(valueOperations.get("auth:ticket:t2")).thenReturn("web|http://localhost:5173/sso/callback|u2");
        try {
            ssoService.consumeTicket("t2", "app");
            fail("client mismatch should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void missingTicketIsRejected() {
        try {
            ssoService.consumeTicket("missing", "web");
            fail("missing ticket should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }
}
