package com.iot.platform.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.common.ApiResponse;
import com.iot.platform.model.ControlRule;
import com.iot.platform.repository.ControlRuleRepository;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.security.datascope.DataScopeRegistry;
import com.iot.platform.service.control.ControlExecutionService;
import com.iot.platform.service.control.ControlRuleService;
import com.iot.platform.service.control.ControlRuleService.SaveOutcome;
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

/** 控制规则 CRUD + 冲突校验 + 启停 + 整组执行。intent=CHECK/DRAFT/ENABLE 三态。 */
@RestController
@RequestMapping("/api/control/rules")
public class ControlRuleController {

    @Autowired
    private ControlRuleService ruleService;
    @Autowired
    private ControlRuleRepository ruleRepository;
    @Autowired
    private DataScopeRegistry dataScopeRegistry;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String kind,
                               @RequestParam(required = false) String trigger,
                               @RequestParam(required = false) Boolean enabled,
                               HttpServletRequest request) {
        ScopeContexts ctx = ScopeContexts.of(request);
        var wrapper = Wrappers.<ControlRule>lambdaQuery();
        if (kind != null && !kind.isEmpty()) wrapper.eq(ControlRule::getKind, kind);
        if (trigger != null && !trigger.isEmpty()) wrapper.eq(ControlRule::getTrigger, trigger);
        if (enabled != null) wrapper.eq(ControlRule::getEnabled, enabled);
        wrapper.orderByDesc(ControlRule::getCreatedAt);
        List<ControlRule> visible = ruleRepository.findAll(wrapper).stream()
                .filter(r -> dataScopeRegistry.canRead("control_rule", r, ctx.subject, ctx.orgCode, ctx.dataScopes))
                .toList();
        return ApiResponse.success(visible);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id, HttpServletRequest request) {
        try {
            ControlRule rule = ruleService.get(id);
            ScopeContexts ctx = ScopeContexts.of(request);
            if (!dataScopeRegistry.canRead("control_rule", rule, ctx.subject, ctx.orgCode, ctx.dataScopes)) {
                return ApiResponse.fail(404, "规则不存在");
            }
            return ApiResponse.success(rule);
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    @PostMapping("/check")
    public ApiResponse<?> check(@RequestBody ControlRule rule, HttpServletRequest request) {
        ScopeContexts ctx = ScopeContexts.of(request);
        SaveOutcome outcome = ruleService.save(fillScope(rule, ctx), ControlRuleService.INTENT_CHECK);
        return ApiResponse.success(outcome.result);
    }

    @RequireRoles({"ADMIN", "OPERATOR"})
    @PostMapping
    public ApiResponse<?> create(@RequestBody ControlRule rule,
                                 @RequestParam(defaultValue = "DRAFT") String intent,
                                 HttpServletRequest request) {
        ScopeContexts ctx = ScopeContexts.of(request);
        SaveOutcome outcome = ruleService.save(fillScope(rule, ctx), intent);
        if (!outcome.saved) {
            return ApiResponse.fail(400, failMessage(outcome));
        }
        return ApiResponse.success(Map.of("rule", outcome.rule, "conflict", outcome.result));
    }

    @RequireRoles({"ADMIN", "OPERATOR"})
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody ControlRule rule,
                                 @RequestParam(defaultValue = "DRAFT") String intent,
                                 HttpServletRequest request) {
        rule.setId(id);
        ScopeContexts ctx = ScopeContexts.of(request);
        SaveOutcome outcome = ruleService.save(fillScope(rule, ctx), intent);
        if (!outcome.saved) {
            return ApiResponse.fail(400, failMessage(outcome));
        }
        return ApiResponse.success(Map.of("rule", outcome.rule, "conflict", outcome.result));
    }

    @RequireRoles({"ADMIN", "OPERATOR"})
    @PostMapping("/{id}/toggle")
    public ApiResponse<?> toggle(@PathVariable String id) {
        try {
            return ApiResponse.success(ruleService.toggle(id));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @RequireRoles({"ADMIN", "OPERATOR"})
    @PostMapping("/{id}/execute")
    public ApiResponse<?> execute(@PathVariable String id) {
        try {
            ControlRule rule = ruleService.get(id);
            String source = "GROUP".equals(rule.getKind())
                    ? ControlExecutionService.SOURCE_GROUP : "MANUAL";
            return ApiResponse.success(ruleService.execute(rule, source));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @RequireRoles({"ADMIN", "OPERATOR"})
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        ruleService.delete(id);
        return ApiResponse.success(null);
    }

    private ControlRule fillScope(ControlRule rule, ScopeContexts ctx) {
        rule.setOrgCode(ctx.orgCode);
        rule.setOwnerUserId(ctx.subject);
        rule.setCreatedBy(ctx.subject);
        return rule;
    }

    private static String failMessage(SaveOutcome outcome) {
        String err = outcome.result.getError();
        return err == null || err.isEmpty() ? "存在控制冲突，未能保存" : err;
    }
}
