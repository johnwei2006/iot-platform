package com.iot.admin.user;

import com.iot.admin.internal.AuthInternalClient;
import com.iot.admin.internal.AuthInternalClient.UserSummary;
import com.iot.admin.org.OrgRepository;
import com.iot.admin.role.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserScopeTest {

    @Mock private AuthInternalClient authClient;
    @Mock private RoleService roleService;
    @Mock private OrgRepository orgRepository;

    private AdminUserController controller;

    private UserSummary user(String id, String orgCode) {
        UserSummary u = new UserSummary();
        u.setId(id);
        u.setOrgCode(orgCode);
        return u;
    }

    @BeforeEach
    void setUp() {
        controller = new AdminUserController(authClient, roleService, orgRepository);
    }

    @Test
    void orgAdminOnlySeesUsersInOwnOrg() {
        HttpServletRequest http = mock(HttpServletRequest.class);
        when(http.getAttribute("superAdmin")).thenReturn(false);
        when(http.getAttribute("org")).thenReturn("0002");
        when(authClient.listUsers()).thenReturn(new ArrayList<>(Arrays.asList(
                user("u1", "0001"), user("u2", "0002"), user("u3", "000201"))));

        List<UserSummary> result = controller.list(http).getData();

        assertEquals(Arrays.asList("u2", "u3"),
                result.stream().map(UserSummary::getId).collect(Collectors.toList()));
    }
}
