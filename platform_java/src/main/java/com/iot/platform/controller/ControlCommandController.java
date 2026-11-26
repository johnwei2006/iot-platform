package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.model.ControlLog;
import com.iot.platform.model.ControlRule;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.service.control.ControlExecutionService;
import com.iot.platform.service.control.ControlLogStateMachine;
import com.iot.platform.service.control.ControlRuleService;
import com.iot.platform.service.control.GatewayEntry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 手动/组合控制下发：风机开关、棉被卷放、开度、组合整组。 */
@RestController
@RequestMapping("/api/control/commands")
public class ControlCommandController {

    @Autowired
    private ControlExecutionService executionService;
    @Autowired
    private ControlRuleService ruleService;
    @Autowired
    private com.iot.platform.service.DeviceCtrlService deviceCtrlService;

    /**
     * 手动下发，两种选设备方式（至少其一）：
     * 1) {"devices":[GatewayEntry...]} 直接给三元组；
     * 2) {"deviceCodes":["..."]} 从 iot_device_ctrl 按设备编码自动映射（经节点表定位网关/通道）。
     * action: ON|OFF|OPEN|CLOSE|STOP|LEVEL；value: 开度或空。
     */
    @RequireRoles({"ADMIN", "OPERATOR"})
    @PostMapping("/devices")
    public ApiResponse<?> manual(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawDevices = (List<Map<String, Object>>) body.get("devices");
            @SuppressWarnings("unchecked")
            List<String> deviceCodes = (List<String>) body.get("deviceCodes");
            String action = String.valueOf(body.get("action"));
            String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<GatewayEntry> devices = rawDevices == null ? null : om.convertValue(rawDevices,
                    om.getTypeFactory().constructCollectionType(List.class, GatewayEntry.class));
            if (devices == null || devices.isEmpty()) {
                devices = deviceCodes == null ? List.of() : deviceCodes.stream()
                        .map(deviceCtrlService::toGatewayEntryByDeviceCode)
                        .toList();
            }
            if (devices == null || devices.isEmpty()) {
                return ApiResponse.fail(400, "请选择受控设备");
            }
            String source = devices.size() > 1 ? ControlExecutionService.SOURCE_GROUP
                    : ControlExecutionService.SOURCE_MANUAL;
            String orgCode = ScopeContexts.of(request).orgCode;
            List<ControlLog> logs = devices.stream()
                    .map(entry -> executionService.executeDevice(entry, action, value, null, null,
                            source, null, orgCode))
                    .toList();
            return ApiResponse.success(Map.of("logs", logs,
                    "result", ControlLogStateMachine.aggregateResult(logs)));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    /** 组合/规则整组执行（规则须已启用）。 */
    @RequireRoles({"ADMIN", "OPERATOR"})
    @PostMapping("/rules/{id}/execute")
    public ApiResponse<?> executeRule(@PathVariable String id) {
        try {
            ControlRule rule = ruleService.get(id);
            String source = "GROUP".equals(rule.getKind())
                    ? ControlExecutionService.SOURCE_GROUP : ControlExecutionService.SOURCE_MANUAL;
            List<ControlLog> logs = ruleService.execute(rule, source);
            return ApiResponse.success(Map.of("logs", logs,
                    "result", ControlLogStateMachine.aggregateResult(logs)));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
