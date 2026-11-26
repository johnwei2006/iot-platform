package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.DeviceCtrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 控制设备管理接口：第三层受控实体，挂在节点下。 */
@RestController
@RequestMapping("/api/control-devices")
public class DeviceCtrlController {

    @Autowired
    private DeviceCtrlService deviceCtrlService;

    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    /** 控制设备列表：按 JWT 数据权限（ds.device_ctrl）过滤；可选 nodeCode 过滤。 */
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String nodeCode,
                               HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        List<DeviceCtrl> devices = deviceCtrlService.listByCriteria(
                dataScopeRegistry.criteriaFor("device_ctrl", ctx.subject, ctx.orgCode, ctx.dataScopes));
        if (nodeCode != null && !nodeCode.isEmpty()) {
            devices = devices.stream().filter(d -> nodeCode.equals(d.getNodeCode())).toList();
        }
        return ApiResponse.success(devices);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        Optional<DeviceCtrl> d = deviceCtrlService.get(id);
        if (d.isPresent()) {
            ScopeContext ctx = scopeContext(request);
            if (!dataScopeRegistry.canRead("device_ctrl", d.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
                return ApiResponse.fail(ResultCode.NOT_FOUND);
            }
            return ApiResponse.success(d.get());
        }
        return ApiResponse.fail(ResultCode.NOT_FOUND);
    }

    /** 新建控制设备，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody DeviceCtrl d, HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(deviceCtrlService.create(d, ctx.subject, ctx.orgCode));
    }

    /** 更新控制设备，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody DeviceCtrl d) {
        try {
            return ApiResponse.success(deviceCtrlService.update(id, d));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    /** 删除控制设备（逻辑删除），仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        deviceCtrlService.delete(id);
        return ApiResponse.success(null);
    }

    /** 控制设备 -> 下发三元组（GatewayEntry）：deviceId=网关编码, nodeId=节点编码, channel。 */
    @GetMapping("/{id}/gateway-entry")
    public ApiResponse<?> gatewayEntry(@PathVariable String id) {
        try {
            DeviceCtrl d = deviceCtrlService.get(id)
                    .orElseThrow(() -> new RuntimeException("控制设备不存在: " + id));
            return ApiResponse.success(deviceCtrlService.toGatewayEntry(d));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ScopeContext scopeContext(HttpServletRequest request) {
        ScopeContext ctx = new ScopeContext();
        ctx.subject = (String) request.getAttribute("subject");
        ctx.orgCode = (String) request.getAttribute("org");
        ctx.dataScopes = (Map<String, String>) request.getAttribute("dataScopes");
        return ctx;
    }

    private static class ScopeContext {
        private String subject;
        private String orgCode;
        private Map<String, String> dataScopes;
    }
}
