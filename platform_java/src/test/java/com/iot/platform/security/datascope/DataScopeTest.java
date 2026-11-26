package com.iot.platform.security.datascope;

import com.iot.platform.model.Gateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DataScopeTest {

    private DataScopeRegistry registry;
    private GatewayDataScopeRule deviceRule;

    @BeforeEach
    public void setUp() {
        deviceRule = new GatewayDataScopeRule();
        registry = new DataScopeRegistry(Collections.<DataScopeRule>singletonList(deviceRule));
    }

    private Map<String, String> ds(String scope) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("device", scope);
        return map;
    }

    @Test
    public void allScopeReturnsNullCriteria() {
        assertNull(registry.criteriaFor("device", "u1", "0001", ds(DataScopeRegistry.SCOPE_ALL)));
        assertTrue(deviceRule.matches(device("000101", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ALL)));
    }

    @Test
    public void orgAndChildrenUsesPrefixMatch() {
        DataScopeQuery query = registry.criteriaFor("device", "u1", "0001",
                ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN));
        assertEquals(DataScopeQuery.Type.ORG_AND_CHILDREN, query.getType());
        assertEquals("0001", query.getValue());

        // 前缀语义：本机构 0001 与子孙 000101/00010103 均可见
        assertTrue(deviceRule.matches(device("0001", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN)));
        assertTrue(deviceRule.matches(device("000101", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN)));
        assertTrue(deviceRule.matches(device("00010103", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN)));
        // 非本机构（前缀不同但共享数字）不可见：0002 与 000101 不互见
        assertFalse(deviceRule.matches(device("0002", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN)));
        assertFalse(deviceRule.matches(device("000101", "u1"), "u1", "000102", ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN)));
    }

    @Test
    public void orgOnlyIsExactMatch() {
        assertTrue(deviceRule.matches(device("0001", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ORG_ONLY)));
        assertFalse(deviceRule.matches(device("000101", "u1"), "u1", "0001", ds(DataScopeRegistry.SCOPE_ORG_ONLY)));
    }

    @Test
    public void selfScopeUsesOwner() {
        assertTrue(deviceRule.matches(device("0001", "u1"), "u1", null, ds(DataScopeRegistry.SCOPE_SELF)));
        assertFalse(deviceRule.matches(device("0001", "u2"), "u1", null, ds(DataScopeRegistry.SCOPE_SELF)));
        // 无机构但 SELF 范围仍按归属判定（不依赖机构）
        DataScopeQuery query = registry.criteriaFor("device", "u1", null, ds(DataScopeRegistry.SCOPE_SELF));
        assertEquals(DataScopeQuery.Type.SELF, query.getType());
        assertEquals("u1", query.getValue());
    }

    @Test
    public void missingScopeOrOrgIsDenied() {
        DataScopeQuery denied = registry.criteriaFor("device", "u1", null,
                ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN));
        assertEquals(DataScopeQuery.Type.DENY_ALL, denied.getType());
        assertFalse(deviceRule.matches(device("0001", "u1"), "u1", null, ds(DataScopeRegistry.SCOPE_ORG_AND_CHILDREN)));
        // 完全无 ds 声明 → 拒绝
        assertFalse(deviceRule.matches(device("0001", "u1"), "u1", "0001", null));
        // 未注册实体
        try {
            registry.criteriaFor("alarm", "u1", "0001", ds(DataScopeRegistry.SCOPE_ALL));
            fail("unregistered entity should fail");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private Gateway device(String orgCode, String owner) {
        Gateway device = new Gateway();
        device.setOrgCode(orgCode);
        device.setOwnerUserId(owner);
        return device;
    }
}
