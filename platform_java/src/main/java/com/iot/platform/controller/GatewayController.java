package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.Gateway;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
public class GatewayController {

    @Autowired
    private GatewayService deviceService;

    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    /** 设备列表：按 JWT 数据权限（ds.device）过滤。 */
    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        List<Gateway> devices = deviceService.listByCriteria(
                dataScopeRegistry.criteriaFor("device", ctx.subject, ctx.orgCode, ctx.dataScopes));
        devices.forEach(this::maskSecret);
        return ApiResponse.success(devices);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        Optional<Gateway> d = deviceService.get(id);
        if (d.isPresent()) {
            ScopeContext ctx = scopeContext(request);
            if (!dataScopeRegistry.canRead("device", d.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
                return ApiResponse.fail(ResultCode.NOT_FOUND);
            }
            maskSecret(d.get());
            return ApiResponse.success(d.get());
        }
        return ApiResponse.fail(ResultCode.NOT_FOUND);
    }

    /** 更新网关认证预共享密钥；body 可为空（自动生成随机密钥）或 {"authSecret":"xxx"}。仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}/secret")
    public ApiResponse<?> updateSecret(@PathVariable String id,
                                       @RequestBody(required = false) Map<String, String> body) {
        try {
            String secret = body == null ? null : body.get("authSecret");
            return ApiResponse.success(deviceService.updateSecret(id, secret));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    private void maskSecret(Gateway d) {
        if (d.getAuthSecret() != null && !d.getAuthSecret().isEmpty()) {
            d.setAuthSecret(GatewayService.SECRET_MASK);
        }
    }

    /** 从 JwtFilter 写入的 request 属性提取数据权限上下文。 */
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

    /** 新建设备，仅 ADMIN；归属绑定创建者与机构。 */
    @RequireRoles({"ADMIN"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody Gateway d, HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(deviceService.create(d, ctx.subject, ctx.orgCode));
    }

    /** 更新设备，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Gateway d) {
        try {
            return ApiResponse.success(deviceService.update(id, d));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    /** 删除设备，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        deviceService.delete(id);
        return ApiResponse.success(null);
    }
}
