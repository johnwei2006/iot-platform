package com.iot.auth.sso;

import com.iot.auth.config.AuthProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SsoSessionServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private AuthProperties properties;
    private SsoSessionService sessionService;

    @Before
    public void setUp() {
        properties = new AuthProperties();
        properties.setRedisPrefix("auth:");
        when(redis.opsForValue()).thenReturn(valueOperations);
        sessionService = new SsoSessionService(properties, redis);
    }

    @Test
    public void createSessionStoresUserIdAndWritesCookie() {
        sessionService.createSession("u1", response);

        verify(valueOperations).set(contains("auth:sso-session:"), eq("u1"),
                anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(response).addHeader(eq("Set-Cookie"), contains("auth_sso="));
        verify(response).addHeader(eq("Set-Cookie"), contains("HttpOnly"));
        verify(response).addHeader(eq("Set-Cookie"), contains("SameSite=Lax"));
        verify(response).addHeader(eq("Set-Cookie"), contains("Path=/"));
    }

    @Test
    public void resolveUserIdReturnsStoredUser() {
        Cookie cookie = new Cookie("auth_sso", "session-1");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(valueOperations.get("auth:sso-session:session-1")).thenReturn("u2");

        assertEquals("u2", sessionService.resolveUserId(request));
    }

    @Test
    public void resolveUserIdReturnsNullWithoutCookie() {
        when(request.getCookies()).thenReturn(null);
        assertNull(sessionService.resolveUserId(request));
    }

    @Test
    public void destroySessionDeletesKeyAndClearsCookie() {
        Cookie cookie = new Cookie("auth_sso", "session-3");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(valueOperations.get("auth:sso-session:session-3")).thenReturn("u3");

        assertEquals("u3", sessionService.destroySession(request, response));
        verify(redis).delete("auth:sso-session:session-3");
        verify(response).addHeader(eq("Set-Cookie"), contains("auth_sso=;"));
    }

    @Test
    public void disabledSessionYieldsNoCookie() {
        properties.getSso().setSessionEnabled(false);
        sessionService.createSession("u1", response);
        verify(response, org.mockito.Mockito.never()).addHeader(anyString(), anyString());
    }
}
