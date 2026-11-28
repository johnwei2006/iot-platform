package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.SensorGroup;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.sensor.SensorGroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/** 传感器组合接口：数据权限复用 sensor 配置。 */
@RestController
@RequestMapping("/api/sensor-groups")
public class SensorGroupController {

    @Autowired
    private SensorGroupService service;

    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(service.listByCriteria(
                dataScopeRegistry.criteriaFor("sensor_group", ctx.subject, ctx.orgCode, ctx.dataScopes)));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        Optional<SensorGroup> group = service.get(id);
        if (!group.isPresent()) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        ScopeContext ctx = scopeContext(request);
        if (!dataScopeRegistry.canRead("sensor_group", group.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        return ApiResponse.success(group.get());
    }

    @RequireRoles({"ADMIN"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody SensorGroup group, HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(service.create(group, ctx.subject, ctx.orgCode));
    }

    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody SensorGroup patch) {
        try {
            return ApiResponse.success(service.update(id, patch));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    @RequireRoles({"ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.success(null);
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
