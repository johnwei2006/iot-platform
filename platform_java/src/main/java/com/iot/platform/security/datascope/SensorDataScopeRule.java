package com.iot.platform.security.datascope;

import com.iot.platform.model.Sensor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 传感器数据权限规则。 */
@Component
public class SensorDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "sensor";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        return DataScopeRuleSupport.resolve(dataScopes, "sensor", subject, orgCode);
    }

    @Override
    public boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes) {
        return entity instanceof Sensor && DataScopeRuleSupport.matches(
                dataScopes, "sensor", subject, orgCode, ((Sensor) entity).getOrgCode(),
                ((Sensor) entity).getOwnerUserId());
    }
}
