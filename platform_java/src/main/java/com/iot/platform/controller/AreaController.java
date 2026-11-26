package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.Area;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.AreaReferenceService;
import com.iot.platform.service.AreaService;
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

/** 区域管理接口：数据权限复用 sensor 配置。 */
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    @Autowired
    private AreaService service;

    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    @Autowired
    private AreaReferenceService referenceService;

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(service.listByCriteria(
                dataScopeRegistry.criteriaFor("area", ctx.subject, ctx.orgCode, ctx.dataScopes)));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        Optional<Area> area = service.get(id);
        if (!area.isPresent()) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        ScopeContext ctx = scopeContext(request);
        if (!dataScopeRegistry.canRead("area", area.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        return ApiResponse.success(area.get());
    }

    @RequireRoles({"ADMIN"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody Area area, HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(service.create(area, ctx.subject, ctx.orgCode));
    }

    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Area patch) {
        try {
            return ApiResponse.success(service.update(id, patch));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    /** 区域引用统计：返回四类实体引用数量，供前端联动面板与删除提示使用。 */
    @GetMapping("/{id}/refs")
    public ApiResponse<?> refs(@PathVariable String id) {
        if (!service.get(id).isPresent()) {
            return ApiResponse.fail(ResultCode.NOT_FOUND);
        }
        return ApiResponse.success(referenceService.countByArea(id));
    }

    @RequireRoles({"ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        Map<String, Long> counts = referenceService.countByArea(id);
        long total = counts.getOrDefault("total", 0L);
        if (total > 0) {
            return ApiResponse.fail(409, "区域已被引用（" + referenceService.describe(counts)
                    + "），请先解除关联后再删除");
        }
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
