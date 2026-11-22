package com.iot.platform.security.datascope;

import com.iot.platform.model.Device;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 设备数据权限规则：机构编码前缀匹配实现"本机构及以下"，
 * 无需查询机构树；未标注机构的存量设备仅对 ALL 范围可见。
 */
@Component
public class DeviceDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "device";
    }

    @Override
    public Criteria criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        String scope = dataScopes == null ? null : dataScopes.get("device");
        if (scope == null) {
            return DataScopeRegistry.DENY_ALL;
        }
        if (DataScopeRegistry.SCOPE_ALL.equals(scope)) {
            return null;
        }
        if (DataScopeRegistry.SCOPE_ORG_AND_CHILDREN.equals(scope)) {
            if (orgCode == null || orgCode.isEmpty()) {
                return DataScopeRegistry.DENY_ALL;
            }
            return Criteria.where("orgCode").regex("^" + Pattern.quote(orgCode));
        }
        if (DataScopeRegistry.SCOPE_ORG_ONLY.equals(scope)) {
            if (orgCode == null || orgCode.isEmpty()) {
                return DataScopeRegistry.DENY_ALL;
            }
            return Criteria.where("orgCode").is(orgCode);
        }
        if (DataScopeRegistry.SCOPE_SELF.equals(scope)) {
            if (subject == null || subject.isEmpty()) {
                return DataScopeRegistry.DENY_ALL;
            }
            return Criteria.where("ownerUserId").is(subject);
        }
        return DataScopeRegistry.DENY_ALL;
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
