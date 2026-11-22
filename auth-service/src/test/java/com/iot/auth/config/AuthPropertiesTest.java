package com.iot.auth.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AuthPropertiesTest {

    @Test
    public void clientLookupAndRedirectWhitelist() {
        AuthProperties properties = new AuthProperties();
        AuthProperties.Client client = new AuthProperties.Client();
        client.setId("web");
        client.setRedirectUris(Arrays.asList("http://localhost:5173/sso/callback"));
        properties.setClients(Arrays.asList(client));

        assertNotNull(properties.findClient("web"));
        assertNull(properties.findClient("unknown"));
        assertTrue(properties.findClient("web").allowsRedirect("http://localhost:5173/sso/callback"));
        assertFalse(properties.findClient("web").allowsRedirect("https://evil.example.com"));
    }

    @Test
    public void serviceKeyLookup() {
        AuthProperties properties = new AuthProperties();
        AuthProperties.ServiceKey key = new AuthProperties.ServiceKey();
        key.setId("dtm");
        key.setSecret("secret");
        properties.setServiceKeys(Arrays.asList(key));

        assertEquals("secret", properties.findServiceSecret("dtm"));
        assertNull(properties.findServiceSecret("unknown"));
    }

    @Test
    public void registeredRedirectMatchesAnyClient() {
        AuthProperties properties = new AuthProperties();
        AuthProperties.Client web = new AuthProperties.Client();
        web.setId("web");
        web.setRedirectUris(Arrays.asList("http://localhost:5173/sso/callback"));
        AuthProperties.Client mini = new AuthProperties.Client();
        mini.setId("mini");
        mini.setRedirectUris(Arrays.asList("mini://sso/callback"));
        properties.setClients(Arrays.asList(web, mini));

        assertTrue(properties.isRegisteredRedirect("http://localhost:5173/sso/callback"));
        assertTrue(properties.isRegisteredRedirect("mini://sso/callback"));
        assertFalse(properties.isRegisteredRedirect("https://evil.example.com"));
        assertFalse(properties.isRegisteredRedirect(null));
    }

    @Test
    public void jwtSecretIsMandatory() {
        try {
            new AuthProperties().validate();
            fail("missing jwt secret should be rejected");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void weakJwtSecretIsRejected() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("too-short");
        try {
            properties.validate();
            fail("weak secret should be rejected");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("32"));
        }
    }

    @Test
    public void cookieDefaultsAreSecure() {
        AuthProperties.Cookie cookie = new AuthProperties().getCookie();
        assertTrue(cookie.isEnabled());
        assertTrue(cookie.isSecure() == false); // 开发环境默认非 HTTPS；生产必须 AUTH_COOKIE_SECURE=true
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.getDomain() == null || cookie.getDomain().isEmpty());
    }
}
