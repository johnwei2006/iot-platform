package com.iot.platform.security;

import com.iot.platform.controller.DeviceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoleInterceptorTest {

    private RoleInterceptor interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = new RoleInterceptor();
    }

    private HandlerMethod handler(String method) throws Exception {
        return new HandlerMethod(new DeviceController(),
                DeviceController.class.getMethod(method, String.class));
    }

    @Test
    public void adminPassesAnnotatedEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/devices/d1");
        request.setAttribute("roles", Arrays.asList("ADMIN"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("delete"));

        assertTrue(pass);
    }

    @Test
    public void nonAdminIsRejected403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/devices/d1");
        request.setAttribute("roles", Arrays.asList("TERMINAL"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("delete"));

        assertFalse(pass);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void noRolesIsRejected403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/devices/d1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, handler("delete"));

        assertFalse(pass);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void unannotatedHandlerPasses() throws Exception {
        HandlerMethod listHandler = new HandlerMethod(new DeviceController(),
                DeviceController.class.getMethod("list", jakarta.servlet.http.HttpServletRequest.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/devices");
        request.setAttribute("roles", Collections.emptyList());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), listHandler));
    }
}
