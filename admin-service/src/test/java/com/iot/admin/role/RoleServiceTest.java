package com.iot.admin.role;

import com.iot.admin.common.AppException;
import com.iot.admin.config.AdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private AdminProperties properties;
    private RoleService roleService;

    @BeforeEach
    public void setUp() {
        properties = new AdminProperties();
        AdminProperties.Entity device = new AdminProperties.Entity();
        device.setCode("device");
        device.setName("设备");
        properties.setEntities(Arrays.asList(device));
        roleService = new RoleService(roleRepository, properties);
    }

    private Role role(String code, Map<String, String> ds) {
        Role role = new Role();
        role.setCode(code);
        role.setName(code);
        role.setDataScopes(ds);
        return role;
    }

    @Test
    public void mergeTakesWidestScopePerEntity() {
        Map<String, String> orgAdminDs = new HashMap<String, String>();
        orgAdminDs.put("device", RoleService.SCOPE_ORG_AND_CHILDREN);
        Map<String, String> viewerDs = new HashMap<String, String>();
        viewerDs.put("device", RoleService.SCOPE_SELF);

        Map<String, String> merged = roleService.mergeDataScopes(Arrays.asList(
                role("ORG_ADMIN", orgAdminDs), role("VIEWER", viewerDs)));

        assertEquals(RoleService.SCOPE_ORG_AND_CHILDREN, merged.get("device"));
    }

    @Test
    public void unknownEntityDefaultsToSelf() {
        Map<String, String> ds = new HashMap<String, String>();
        ds.put("device", RoleService.SCOPE_ALL);

        Map<String, String> merged = roleService.mergeDataScopes(
                Collections.singletonList(role("R1", ds)));

        // 已注册实体取角色配置；注册表中的另一实体（无）默认 SELF
        assertEquals(RoleService.SCOPE_ALL, merged.get("device"));
        assertEquals(1, merged.size());
    }

    @Test
    public void createValidatesEntityRegistered() {
        when(roleRepository.findByCode("R1")).thenReturn(Optional.empty());
        Map<String, String> ds = new HashMap<String, String>();
        ds.put("ghost_entity", RoleService.SCOPE_ALL);

        try {
            roleService.create("R1", "测试角色", null, ds);
            fail("unknown entity should be rejected");
        } catch (AppException e) {
            assertEquals(400, e.getResultCode().getCode());
        }
    }

    @Test
    public void createValidatesScopeValue() {
        when(roleRepository.findByCode("R1")).thenReturn(Optional.empty());
        Map<String, String> ds = new HashMap<String, String>();
        ds.put("device", "EVERYTHING");

        try {
            roleService.create("R1", "测试角色", null, ds);
            fail("invalid scope value should be rejected");
        } catch (AppException e) {
            assertEquals(400, e.getResultCode().getCode());
        }
    }

    @Test
    public void updateLocksBuiltInRole() {
        Role builtIn = role("SUPER_ADMIN", new HashMap<String, String>());
        builtIn.setBuiltIn(true);
        builtIn.setId("r-super");
        when(roleRepository.findById("r-super")).thenReturn(Optional.of(builtIn));

        try {
            roleService.update("r-super", "改名", Arrays.asList("m1"), null, null);
            fail("built-in role menu change should be rejected");
        } catch (AppException e) {
            assertEquals(403, e.getResultCode().getCode());
        }
    }
}
