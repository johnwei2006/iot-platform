package com.iot.platform.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.common.ApiResponse;
import com.iot.platform.model.ControlLog;
import com.iot.platform.repository.ControlLogRepository;
import com.iot.platform.security.datascope.DataScopeRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 控制记录列表与详情（来源、设备反馈、执行结果）。 */
@RestController
@RequestMapping("/api/control/logs")
public class ControlLogController {

    @Autowired
    private ControlLogRepository logRepository;
    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String deviceId,
                               @RequestParam(required = false) String ruleId,
                               @RequestParam(required = false) String result,
                               @RequestParam(required = false) String source,
                               HttpServletRequest request) {
        ScopeContexts ctx = ScopeContexts.of(request);
        var wrapper = Wrappers.<ControlLog>lambdaQuery();
        if (deviceId != null && !deviceId.isEmpty()) wrapper.eq(ControlLog::getDeviceId, deviceId);
        if (ruleId != null && !ruleId.isEmpty()) wrapper.eq(ControlLog::getRuleId, ruleId);
        if (result != null && !result.isEmpty()) wrapper.eq(ControlLog::getResult, result);
        if (source != null && !source.isEmpty()) wrapper.eq(ControlLog::getSource, source);
        wrapper.orderByDesc(ControlLog::getCreatedAt);
        List<ControlLog> visible = logRepository.findAll(wrapper).stream()
                .filter(l -> dataScopeRegistry.canRead("control_log", l, ctx.subject, ctx.orgCode, ctx.dataScopes))
                .toList();
        return ApiResponse.success(visible);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        return logRepository.findById(id)
                .filter(l -> {
                    ScopeContexts ctx = ScopeContexts.of(request);
                    return dataScopeRegistry.canRead("control_log", l, ctx.subject, ctx.orgCode, ctx.dataScopes);
                })
                .map(l -> ApiResponse.success((Object) l))
                .orElseGet(() -> ApiResponse.fail(404, "记录不存在"));
    }
}
