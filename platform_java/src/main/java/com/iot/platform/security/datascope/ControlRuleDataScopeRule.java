package com.iot.platform.security.datascope;

import com.iot.platform.model.ControlRule;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 控制规则数据权限规则，模式与 GatewayDataScopeRule 相同（entityCode=control_rule）。 */
@Component
public class ControlRuleDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "control_rule";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        String scope = dataScopes == null ? null : dataScopes.get("control_rule");
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
        if (!(entity instanceof ControlRule)) {
            return false;
        }
        ControlRule rule = (ControlRule) entity;
        String scope = dataScopes == null ? null : dataScopes.get("control_rule");
        if (scope == null) {
            return false;
        }
        if (DataScopeRegistry.SCOPE_ALL.equals(scope)) {
            return true;
        }
        if (DataScopeRegistry.SCOPE_ORG_AND_CHILDREN.equals(scope)) {
            return orgCode != null && rule.getOrgCode() != null
                    && rule.getOrgCode().startsWith(orgCode);
        }
        if (DataScopeRegistry.SCOPE_ORG_ONLY.equals(scope)) {
            return orgCode != null && orgCode.equals(rule.getOrgCode());
        }
        if (DataScopeRegistry.SCOPE_SELF.equals(scope)) {
            return subject != null && subject.equals(rule.getOwnerUserId());
        }
        return false;
    }
}
