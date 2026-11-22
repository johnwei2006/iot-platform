package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.model.Task;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 任务：分配（发站内消息）、列表（待办/已完成/超时态标记）、提交完成。 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /** 列表：status=TODO|DONE 可选；机构级数据权限（orgCode 前缀匹配，本机构及以下）。 */
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String assignee,
                               HttpServletRequest request) {
        List<Task> tasks = taskService.list(status, assignee);
        String org = ScopeContexts.of(request).orgCode;
        if (org != null && !org.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> t.getOrgCode() != null && t.getOrgCode().startsWith(org))
                    .toList();
        }
        Date now = new Date();
        List<Map<String, Object>> withMark = tasks.stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("task", t);
                    m.put("overdueMark", TaskService.overdueMark(t, now));
                    return m;
                }).toList();
        return ApiResponse.success(withMark);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id) {
        Task task = taskService.get(id);
        Map<String, Object> m = new HashMap<>();
        m.put("task", task);
        m.put("overdueMark", TaskService.overdueMark(task, new Date()));
        return ApiResponse.success(m);
    }

    @RequireRoles({"ADMIN", "OPERATOR"})
    @PostMapping
    public ApiResponse<?> assign(@RequestBody Task task, HttpServletRequest request) {
        try {
            ScopeContexts ctx = ScopeContexts.of(request);
            return ApiResponse.success(taskService.assign(task, ctx.subject, ctx.orgCode));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<?> complete(@PathVariable String id,
                                   @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body == null ? null : body.get("doneNote");
            Task task = taskService.complete(id, note);
            Map<String, Object> m = new HashMap<>();
            m.put("task", task);
            m.put("overdueMark", TaskService.overdueMark(task, new Date()));
            return ApiResponse.success(m);
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}