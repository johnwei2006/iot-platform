package com.iot.platform.security.datascope;

import java.util.Map;

/** 数据权限规则共用实现：按 entityCode 从 dataScopes 取范围并构造查询/匹配。 */
final class DataScopeRuleSupport {

    private DataScopeRuleSupport() {
    }

    /** 构造查询条件；返回 null 表示不过滤（ALL），DENY_ALL 表示拒绝。 */
    static DataScopeQuery resolve(Map<String, String> dataScopes, String entityCode,
                                  String subject, String orgCode) {
        String scope = dataScopes == null ? null : dataScopes.get(entityCode);
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

    /** 单实体读取判定，语义与 resolve 一致。 */
    static boolean matches(Map<String, String> dataScopes, String entityCode,
                           String subject, String orgCode, String entityOrgCode,
                           String entityOwnerUserId) {
        String scope = dataScopes == null ? null : dataScopes.get(entityCode);
        if (scope == null) {
            return false;
        }
        if (DataScopeRegistry.SCOPE_ALL.equals(scope)) {
            return true;
        }
        if (DataScopeRegistry.SCOPE_ORG_AND_CHILDREN.equals(scope)) {
            return orgCode != null && entityOrgCode != null
                    && entityOrgCode.startsWith(orgCode);
        }
        if (DataScopeRegistry.SCOPE_ORG_ONLY.equals(scope)) {
            return orgCode != null && orgCode.equals(entityOrgCode);
        }
        if (DataScopeRegistry.SCOPE_SELF.equals(scope)) {
            return subject != null && subject.equals(entityOwnerUserId);
        }
        return false;
    }
}
