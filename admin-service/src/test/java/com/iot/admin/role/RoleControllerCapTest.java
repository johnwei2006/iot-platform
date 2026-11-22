package com.iot.admin.role;

import com.iot.admin.common.AppException;
import com.iot.admin.config.AdminProperties;
import com.iot.admin.product.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleControllerCapTest {

    @Mock
    private RoleService roleService;
    @Mock
    private ProductService productService;

    private AdminProperties properties;
    private RoleController controller;

    @BeforeEach
    void setUp() {
        properties = new AdminProperties();
        AdminProperties.Entity entity = new AdminProperties.Entity();
        entity.setCode("device");
        entity.setName("设备");
        properties.setEntities(new ArrayList<>(List.of(entity)));
        controller = new RoleController(roleService, properties, productService);
    }

    private HttpServletRequest orgRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("superAdmin")).thenReturn(false);
        when(request.getAttribute("org")).thenReturn("0002");
        return request;
    }

    @Test
    void orgAdminCannotGrantMenuOutsideAssignedProducts() {
        HttpServletRequest request = orgRequest();
        when(productService.allowedMenuIdsForOrg("0002")).thenReturn(Set.of("m-iot"));
        RoleController.SaveRoleRequest body = new RoleController.SaveRoleRequest();
        body.setCode("ORG_ROLE");
        body.setMenuIds(List.of("m-iot", "m-system"));

        assertThrows(AppException.class, () -> controller.create(body, request));
        verify(roleService, never()).create(anyString(), any(), anyList(), anyMap());
    }

    @Test
    void orgAdminRoleScopesAreClampedToOwnOrg() {
        HttpServletRequest request = orgRequest();
        when(productService.allowedMenuIdsForOrg("0002")).thenReturn(Set.of("m-iot"));
        RoleController.SaveRoleRequest body = new RoleController.SaveRoleRequest();
        body.setCode("ORG_ROLE");
        body.setMenuIds(List.of("m-iot"));
        body.setDataScopes(new java.util.HashMap<>(java.util.Map.of("device", "ALL")));
        when(roleService.create(anyString(), any(), anyList(), anyMap())).thenReturn(new Role());

        controller.create(body, request);

        verify(roleService).create("ORG_ROLE", null, List.of("m-iot"),
                java.util.Map.of("device", "ORG_AND_CHILDREN"));
    }
}
