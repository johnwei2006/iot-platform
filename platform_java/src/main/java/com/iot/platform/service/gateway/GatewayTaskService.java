package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.model.GatewayTask;
import com.iot.platform.repository.GatewayTaskRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 网关本地任务 CRUD 与 0x25 任务列表映射。 */
@Service
public class GatewayTaskService {

    private final GatewayTaskRepository repository;

    public GatewayTaskService(GatewayTaskRepository repository) { this.repository = repository; }

    public List<GatewayTask> list(String gatewayCode) {
        var q = Wrappers.<GatewayTask>lambdaQuery();
        if (gatewayCode != null && !gatewayCode.isEmpty()) q.eq(GatewayTask::getGatewayCode, gatewayCode);
        q.orderByAsc(GatewayTask::getTaskNo);
        return repository.findAll(q);
    }

    public GatewayTask save(GatewayTask task) {
        if (task.getGatewayCode() == null || task.getGatewayCode().isEmpty()) {
            throw new IllegalArgumentException("gatewayCode 不能为空");
        }
        if (task.getTaskNo() == null || task.getTaskNo() < 1 || task.getTaskNo() > 255) {
            throw new IllegalArgumentException("任务编号必须在 1~255 之间");
        }
        if (task.getTaskName() == null || task.getTaskName().isEmpty()) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        if (task.getEnabled() == null) task.setEnabled(true);
        if (task.getTriggerType() == null || task.getTriggerType().isEmpty()) task.setTriggerType("TIME");
        if (task.getDeployStatus() == null) task.setDeployStatus("PENDING");
        if (task.getId() == null) task.setId(UUID.randomUUID().toString());
        return repository.save(task);
    }

    public void delete(String id) { repository.deleteById(id); }

    /** 按 0x25 schema 构造任务列表报文；前端可传空列表用于清空网关任务。 */
    public Map<String, Object> buildTaskListPayload(List<GatewayTask> tasks) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (GatewayTask t : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", t.getTaskNo());
            item.put("enabled", Boolean.TRUE.equals(t.getEnabled()) ? 1 : 0);
            item.put("triggerType", "TIME".equalsIgnoreCase(t.getTriggerType() == null ? "" : t.getTriggerType()) ? 1 : 2);
            item.put("triggerTime", t.getTriggerTime() == null || t.getTriggerTime().isEmpty() ? "00:00:00" : t.getTriggerTime());
            item.put("triggerSize", t.getTriggers() == null || t.getTriggers().isEmpty() ? 0 : countJsonItems(t.getTriggers()));
            item.put("triggers", parseList(t.getTriggers()));
            item.put("actionSize", t.getActions() == null || t.getActions().isEmpty() ? 0 : countJsonItems(t.getActions()));
            item.put("actions", parseList(t.getActions()));
            items.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskCount", items.size());
        payload.put("tasks", items);
        return payload;
    }

    private List<Object> parseList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("任务条件/动作不是合法 JSON 数组", e);
        }
    }

    private int countJsonItems(String json) { return parseList(json).size(); }
}
