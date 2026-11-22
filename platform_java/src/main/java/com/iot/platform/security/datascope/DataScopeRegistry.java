package com.iot.platform.security.datascope;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据权限规则注册表：自动收集所有 {@link DataScopeRule} bean（实体编码 → 规则）。
 * 扩展新业务实体：实现 DataScopeRule 并注册为 bean，admin-service 的 admin.entities
 * 同步注册实体编码即可配置分配。
 */
@Component
public class DataScopeRegistry {

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_ORG_AND_CHILDREN = "ORG_AND_CHILDREN";
    public static final String SCOPE_ORG_ONLY = "ORG_ONLY";
    public static final String SCOPE_SELF = "SELF";

    /** 永假条件（明确拒绝时使用） */
    public static final Criteria DENY_ALL = Criteria.where("_id").is("__scope_denied__");

    private final Map<String, DataScopeRule> rules = new HashMap<String, DataScopeRule>();

    public DataScopeRegistry(List<DataScopeRule> ruleBeans) {
        for (DataScopeRule rule : ruleBeans) {
            rules.put(rule.entityCode(), rule);
        }
    }

    public DataScopeRule ruleFor(String entity) {
        DataScopeRule rule = rules.get(entity);
        if (rule == null) {
            throw new IllegalArgumentException("no data-scope rule registered for entity: " + entity);
        }
        return rule;
    }

    public boolean hasRule(String entity) {
        return rules.containsKey(entity);
    }

    public Criteria criteriaFor(String entity, String subject, String orgCode,
                                Map<String, String> dataScopes) {
        return ruleFor(entity).criteria(subject, orgCode, dataScopes);
    }

    public boolean canRead(String entity, Object entityObject, String subject,
                           String orgCode, Map<String, String> dataScopes) {
        return ruleFor(entity).matches(entityObject, subject, orgCode, dataScopes);
    }
}
