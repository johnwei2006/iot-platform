package com.iot.platform.security.datascope;

import com.iot.platform.model.Node;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 节点数据权限规则。 */
@Component
public class NodeDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "node";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        return DataScopeRuleSupport.resolve(dataScopes, "node", subject, orgCode);
    }

    @Override
    public boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes) {
        return entity instanceof Node && DataScopeRuleSupport.matches(
                dataScopes, "node", subject, orgCode, ((Node) entity).getOrgCode(),
                ((Node) entity).getOwnerUserId());
    }
}
