package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.Sensor;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.sensor.SensorService;
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

/** 传感器管理接口：第三层采集实体，挂在节点下。 */
@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    /** 传感器列表：按 JWT 数据权限（ds.sensor）过滤；可选 nodeCode、areaId 过滤。 */
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String nodeCode,
                               @RequestParam(required = false) String areaId,
                               HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        List<Sensor> sensors = sensorService.listByCriteria(
                dataScopeRegistry.criteriaFor("sensor", ctx.subject, ctx.orgCode, ctx.dataScopes), areaId);
        if (nodeCode != null && !nodeCode.isEmpty()) {
            sensors = sensors.stream().filter(s -> nodeCode.equals(s.getNodeCode())).toList();
        }
        return ApiResponse.success(sensors);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        Optional<Sensor> s = sensorService.get(id);
        if (s.isPresent()) {
            ScopeContext ctx = scopeContext(request);
            if (!dataScopeRegistry.canRead("sensor", s.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
                return ApiResponse.fail(ResultCode.NOT_FOUND);
            }
            return ApiResponse.success(s.get());
        }
        return ApiResponse.fail(ResultCode.NOT_FOUND);
    }

    /** 传感器历史曲线：按 id 查库并做数据权限校验，时间范围缺省为最近 24 小时。 */
    @GetMapping("/{id}/history")
    public ApiResponse<?> history(@PathVariable String id,
                                  @RequestParam(required = false) Long from,
                                  @RequestParam(required = false) Long to,
                                  HttpServletRequest request) {
        Optional<Sensor> sensor = sensorService.get(id);
        if (!sensor.isPresent()) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        ScopeContext ctx = scopeContext(request);
        if (!dataScopeRegistry.canRead("sensor", sensor.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        long end = to == null ? System.currentTimeMillis() : to;
        long start = from == null ? end - 24L * 60 * 60 * 1000 : from;
        return ApiResponse.success(sensorService.history(sensor.get().getSensorCode(), start, end));
    }

    /** 新建传感器，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody Sensor s, HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(sensorService.create(s, ctx.subject, ctx.orgCode));
    }

    /** 更新传感器，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Sensor s) {
        try {
            return ApiResponse.success(sensorService.update(id, s));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    /** 删除传感器（逻辑删除），仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        sensorService.delete(id);
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
