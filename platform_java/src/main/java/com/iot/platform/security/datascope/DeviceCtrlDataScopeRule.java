package com.iot.platform.security.datascope;

import com.iot.platform.model.DeviceCtrl;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 控制设备数据权限规则（entityCode=device_ctrl，避免与网关的 "device" 冲突）。 */
@Component
public class DeviceCtrlDataScopeRule implements DataScopeRule {

    @Override
    public String entityCode() {
        return "device_ctrl";
    }

    @Override
    public DataScopeQuery criteria(String subject, String orgCode, Map<String, String> dataScopes) {
        return DataScopeRuleSupport.resolve(dataScopes, "device_ctrl", subject, orgCode);
    }

    @Override
    public boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes) {
        return entity instanceof DeviceCtrl && DataScopeRuleSupport.matches(
                dataScopes, "device_ctrl", subject, orgCode, ((DeviceCtrl) entity).getOrgCode(),
                ((DeviceCtrl) entity).getOwnerUserId());
    }
}
