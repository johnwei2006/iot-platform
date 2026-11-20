package com.iot.auth.config;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Test(expected = IllegalStateException.class)
    public void jwtSecretIsMandatory() {
        new AuthProperties().validate();
    }

    @Test
    public void weakJwtSecretIsRejected() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("too-short");
        try {
            properties.validate();
            org.junit.Assert.fail("weak secret should be rejected");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("32"));
        }
    }

    private void assertNotNull(Object value) {
        org.junit.Assert.assertNotNull(value);
    }
}
