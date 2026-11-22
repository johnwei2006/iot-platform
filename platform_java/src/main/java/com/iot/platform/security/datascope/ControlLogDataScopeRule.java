package com.iot.platform.security.datascope;

import com.iot.platform.model.ControlLog;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 控制记录数据权限规则（entityCode=control_log）。 */
@Component
public class ControlLogDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "control_log";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        String scope = dataScopes == null ? null : dataScopes.get("control_log");
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
        if (!(entity instanceof ControlLog)) {
            return false;
        }
        ControlLog log = (ControlLog) entity;
        String scope = dataScopes == null ? null : dataScopes.get("control_log");
        if (scope == null) {
            return false;
        }
        if (DataScopeRegistry.SCOPE_ALL.equals(scope)) {
            return true;
        }
        if (DataScopeRegistry.SCOPE_ORG_AND_CHILDREN.equals(scope)) {
            return orgCode != null && log.getOrgCode() != null
                    && log.getOrgCode().startsWith(orgCode);
        }
        if (DataScopeRegistry.SCOPE_ORG_ONLY.equals(scope)) {
            return orgCode != null && orgCode.equals(log.getOrgCode());
        }
        if (DataScopeRegistry.SCOPE_SELF.equals(scope)) {
            // 控制记录无用户归属列，SELF 范围暂无归属依据，保守拒绝（与原型"记录按机构可见"一致）
            return false;
        }
        return false;
    }
}
