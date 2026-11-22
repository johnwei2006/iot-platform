package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.model.MetricThreshold;
import com.iot.platform.service.ThresholdService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 测点阈值：按设备查询、upsert（唯一键 device+metric+station）、删除。 */
@RestController
@RequestMapping("/api/thresholds")
public class ThresholdController {

    @Autowired
    private ThresholdService thresholdService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam String deviceId) {
        return ApiResponse.success(thresholdService.listByDevice(deviceId));
    }

    @PostMapping
    public ApiResponse<?> upsert(@RequestBody MetricThreshold threshold, HttpServletRequest request) {
        try {
            ScopeContexts ctx = ScopeContexts.of(request);
            threshold.setOrgCode(ctx.orgCode);
            threshold.setUpdatedBy(ctx.subject);
            return ApiResponse.success(thresholdService.upsert(threshold));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        thresholdService.delete(id);
        return ApiResponse.success(null);
    }
}