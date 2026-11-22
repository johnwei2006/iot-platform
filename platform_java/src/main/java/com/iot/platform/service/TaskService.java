package com.iot.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.Message;
import com.iot.platform.model.Task;
import com.iot.platform.repository.MessageRepository;
import com.iot.platform.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 任务服务：分配（创建即发站内消息）、列表（待办/已完成/超时态）、提交完成。
 * 超时判定在读取时计算：TODO 且 dueTime 已过 = 已超时；DONE 且 doneTime > dueTime = 曾超时。
 */
@Service
public class TaskService {

    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_DONE = "DONE";

    private final TaskRepository taskRepository;
    private final MessageRepository messageRepository;

    public TaskService(TaskRepository taskRepository, MessageRepository messageRepository) {
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
    }

    /** 创建任务并给执行人发站内消息。 */
    public Task assign(Task task, String createdBy, String orgCode) {
        task.setId(UUID.randomUUID().toString());
        task.setStatus(STATUS_TODO);
        task.setCreatedBy(createdBy);
        task.setOrgCode(orgCode);
        task.setCreatedAt(new Date());
        Task saved = taskRepository.save(task);
        notify(saved.getId(), saved.getAssigneeUserId(), "TASK", "新任务：" + saved.getTitle(),
                taskBrief(saved));
        return saved;
    }

    /** 提交完成：记录完成时间与说明；超时完成后仍标记 DONE（曾超时由读取侧计算）。 */
    public Task complete(String id, String doneNote) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (STATUS_DONE.equals(task.getStatus())) {
            throw new IllegalStateException("任务已完成");
        }
        task.setStatus(STATUS_DONE);
        task.setDoneTime(new Date());
        task.setDoneNote(doneNote);
        Task saved = taskRepository.save(task);
        notify(saved.getId(), saved.getCreatedBy(), "TASK", "任务已完成：" + saved.getTitle(),
                "完成时间 " + saved.getDoneTime() + (doneNote == null ? "" : " · " + doneNote));
        return saved;
    }

    public Task get(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    }

    /** 任务列表：可按状态与执行人筛选（权限过滤由 Controller 的 ds.task 数据权限完成）。 */
    public List<Task> list(String status, String assignee) {
        var wrapper = Wrappers.<Task>lambdaQuery();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Task::getStatus, status);
        }
        if (assignee != null && !assignee.isEmpty()) {
            wrapper.eq(Task::getAssigneeUserId, assignee);
        }
        wrapper.orderByDesc(Task::getCreatedAt);
        return taskRepository.findAll(wrapper);
    }

    /** 超时态计算（读取侧）：TODO 且已过期；DONE 且完成晚于截止。 */
    public static String overdueMark(Task task, Date now) {
        if (task.getDueTime() == null) {
            return "";
        }
        if (STATUS_DONE.equals(task.getStatus())) {
            return task.getDoneTime() != null && task.getDoneTime().after(task.getDueTime()) ? "LATE_DONE" : "";
        }
        return now.after(task.getDueTime()) ? "OVERDUE" : "";
    }

    private static String taskBrief(Task task) {
        return "截止 " + task.getDueTime() + (task.getContent() == null ? "" : " · " + task.getContent());
    }

    private void notify(String refId, String userId, String type, String title, String content) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        Message m = new Message();
        m.setId(UUID.randomUUID().toString());
        m.setUserId(userId);
        m.setType(type);
        m.setRefId(refId);
        m.setTitle(title);
        m.setContent(content);
        m.setCreatedAt(new Date());
        messageRepository.save(m);
    }
}