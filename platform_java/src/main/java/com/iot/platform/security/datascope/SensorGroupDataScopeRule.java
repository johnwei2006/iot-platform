package com.iot.platform.security.datascope;

import com.iot.platform.model.SensorGroup;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 传感器组合数据权限规则：实体编码独立，权限配置复用 sensor 键。 */
@Component
public class SensorGroupDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "sensor_group";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        return DataScopeRuleSupport.resolve(dataScopes, "sensor", subject, orgCode);
    }

    @Override
    public boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes) {
        return entity instanceof SensorGroup && DataScopeRuleSupport.matches(
                dataScopes, "sensor", subject, orgCode,
                ((SensorGroup) entity).getOrgCode(), ((SensorGroup) entity).getOwnerUserId());
    }
}
