package com.iot.platform.security.datascope;

import com.iot.platform.model.Area;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 区域数据权限规则：实体编码独立，权限配置复用 sensor 键。 */
@Component
public class AreaDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "area";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        return DataScopeRuleSupport.resolve(dataScopes, "sensor", subject, orgCode);
    }

    @Override
    public boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes) {
        return entity instanceof Area && DataScopeRuleSupport.matches(
                dataScopes, "sensor", subject, orgCode,
                ((Area) entity).getOrgCode(), ((Area) entity).getOwnerUserId());
    }
}
