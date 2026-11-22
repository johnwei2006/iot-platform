package com.iot.platform.security.datascope;

import com.iot.platform.model.Device;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 设备数据权限规则：机构编码前缀匹配实现"本机构及以下"，无需查询机构树。 */
@Component
public class DeviceDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "device";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        String scope = dataScopes == null ? null : dataScopes.get("device");
        if (scope == null) {
            return DataScopeQuery.denyAll();
        }
        if (DataScopeRegistry.SCOPE_ALL.equals(scope)) {
            return null;
        }
        if (DataScopeRegistry.SCOPE_ORG_AND_CHILDREN.equals(scope)) {
            if (orgCode == null || orgCode.isEmpty()) {
                return DataScopeQuery.denyAll();
            }
            return DataScopeQuery.orgPrefix(orgCode);
        }
        if (DataScopeRegistry.SCOPE_ORG_ONLY.equals(scope)) {
            if (orgCode == null || orgCode.isEmpty()) {
                return DataScopeQuery.denyAll();
            }
            return DataScopeQuery.org(orgCode);
        }
        if (DataScopeRegistry.SCOPE_SELF.equals(scope)) {
            if (subject == null || subject.isEmpty()) {
                return DataScopeQuery.denyAll();
            }
            return DataScopeQuery.self(subject);
        }
        return DataScopeQuery.denyAll();
    }

    @Override
    public boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes) {
        if (!(entity instanceof Device)) {
            return false;
        }
        Device device = (Device) entity;
        String scope = dataScopes == null ? null : dataScopes.get("device");
        if (scope == null) {
            return false;
        }
        if (DataScopeRegistry.SCOPE_ALL.equals(scope)) {
            return true;
        }
        if (DataScopeRegistry.SCOPE_ORG_AND_CHILDREN.equals(scope)) {
            return orgCode != null && device.getOrgCode() != null
                    && device.getOrgCode().startsWith(orgCode);
        }
        if (DataScopeRegistry.SCOPE_ORG_ONLY.equals(scope)) {
            return orgCode != null && orgCode.equals(device.getOrgCode());
        }
        if (DataScopeRegistry.SCOPE_SELF.equals(scope)) {
            return subject != null && subject.equals(device.getOwnerUserId());
        }
        return false;
    }
}
