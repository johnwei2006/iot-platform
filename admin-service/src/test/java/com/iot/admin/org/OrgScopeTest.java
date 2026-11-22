package com.iot.admin.org;

import com.iot.admin.common.AppException;
import com.iot.admin.internal.AuthInternalClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgScopeTest {

    @Mock private OrgService orgService;
    @Mock private OrgRepository orgRepository;
    @Mock private AuthInternalClient authClient;

    private OrgController controller;

    private Org org(String id, String code) {
        Org o = new Org();
        o.setId(id);
        o.setCode(code);
        return o;
    }

    @BeforeEach
    void setUp() {
        controller = new OrgController(orgService, orgRepository, authClient);
    }

    private HttpServletRequest orgAdmin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("superAdmin")).thenReturn(false);
        when(request.getAttribute("org")).thenReturn("0002");
        return request;
    }

    @Test
    void orgAdminListIsScopedToOwnOrgAndChildren() {
        when(orgService.listAll()).thenReturn(new java.util.ArrayList<>(Arrays.asList(
                org("o1", "0001"), org("o2", "0002"), org("o3", "000201"))));

        List<Org> result = controller.list(orgAdmin()).getData();

        assertEquals(Arrays.asList("0002", "000201"),
                result.stream().map(Org::getCode).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void orgAdminCannotUpdateOtherOrg() {
        when(orgRepository.findById("o1")).thenReturn(Optional.of(org("o1", "0001")));
        OrgController.UpdateOrgRequest body = new OrgController.UpdateOrgRequest();
        body.setName("hacked");

        assertThrows(AppException.class, () -> controller.update("o1", body, orgAdmin()));
    }
}
