package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.Node;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.node.NodeService;
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

/** 节点管理接口：第二层实体，挂在网关下。 */
@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    /** 节点列表：按 JWT 数据权限（ds.node）过滤；可选 gatewayCode、areaId 过滤。 */
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String gatewayCode,
                               @RequestParam(required = false) String areaId,
                               HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        List<Node> nodes = nodeService.listByCriteria(
                dataScopeRegistry.criteriaFor("node", ctx.subject, ctx.orgCode, ctx.dataScopes), areaId);
        if (gatewayCode != null && !gatewayCode.isEmpty()) {
            nodes = nodes.stream().filter(n -> gatewayCode.equals(n.getGatewayCode())).toList();
        }
        return ApiResponse.success(nodes);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        Optional<Node> n = nodeService.get(id);
        if (n.isPresent()) {
            ScopeContext ctx = scopeContext(request);
            if (!dataScopeRegistry.canRead("node", n.get(), ctx.subject, ctx.orgCode, ctx.dataScopes)) {
                return ApiResponse.fail(ResultCode.NOT_FOUND);
            }
            return ApiResponse.success(n.get());
        }
        return ApiResponse.fail(ResultCode.NOT_FOUND);
    }

    /** 新建节点，仅 ADMIN；归属绑定创建者与机构。 */
    @RequireRoles({"ADMIN"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody Node n, HttpServletRequest request) {
        ScopeContext ctx = scopeContext(request);
        return ApiResponse.success(nodeService.create(n, ctx.subject, ctx.orgCode));
    }

    /** 更新节点，仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Node n) {
        try {
            return ApiResponse.success(nodeService.update(id, n));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    /** 删除节点（逻辑删除），仅 ADMIN。 */
    @RequireRoles({"ADMIN"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        nodeService.delete(id);
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
