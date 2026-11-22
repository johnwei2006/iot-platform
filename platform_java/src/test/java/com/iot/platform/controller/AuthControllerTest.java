package com.iot.platform.controller;

import com.iot.platform.security.AuthServiceClient;
import com.iot.platform.security.AuthServiceException;
import com.iot.platform.security.JwtFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class))
public class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthServiceClient authServiceClient;

    @Test
    public void loginForwardsBodyAndReturnsUpstreamResponse() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"secret\"}";
        when(authServiceClient.post(eq("/api/auth/login"), eq(body))).thenReturn(
                new AuthServiceClient.ProxyResponse(200,
                        "{\"code\":200,\"data\":{\"accessToken\":\"jwt\"}}"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt"));

        verify(authServiceClient).post("/api/auth/login", body);
    }

    @Test
    public void refreshAndLogoutAreProxiedToo() throws Exception {
        when(authServiceClient.post(eq("/api/auth/refresh"), eq("{}")))
                .thenReturn(new AuthServiceClient.ProxyResponse(200, "{\"code\":200}"));
        when(authServiceClient.post(eq("/api/auth/logout"), eq("{}")))
                .thenReturn(new AuthServiceClient.ProxyResponse(200, "{\"code\":200}"));

        mvc.perform(post("/api/auth/refresh")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isOk());

        verify(authServiceClient).post("/api/auth/refresh", "{}");
        verify(authServiceClient).post("/api/auth/logout", "{}");
    }

    @Test
    public void preservesUpstreamBusinessError() throws Exception {
        when(authServiceClient.post(eq("/api/auth/login"), eq("{}")))
                .thenReturn(new AuthServiceClient.ProxyResponse(401,
                        "{\"code\":401,\"message\":\"bad credentials\"}"));

        mvc.perform(post("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("bad credentials"));
    }

    @Test
    public void authServiceUnavailableReturns503() throws Exception {
        when(authServiceClient.post(eq("/api/auth/login"), eq("{}")))
                .thenThrow(AuthServiceException.unavailable("auth service down"));

        mvc.perform(post("/api/auth/login"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(503));
    }
}
