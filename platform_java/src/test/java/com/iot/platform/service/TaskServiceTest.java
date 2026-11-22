package com.iot.platform.service;

import com.iot.platform.model.Message;
import com.iot.platform.model.Task;
import com.iot.platform.repository.MessageRepository;
import com.iot.platform.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 任务域单测：分配发消息、提交完成、超时态标记。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MessageRepository messageRepository;

    private TaskService service() {
        return new TaskService(taskRepository, messageRepository);
    }

    private Task sample() {
        Task t = new Task();
        t.setTitle("1 号棚查苗");
        t.setAssigneeUserId("worker-1");
        t.setDueTime(new Date(System.currentTimeMillis() + 3600_000));
        return t;
    }

    @Test
    void assignCreatesTaskAndNotifiesAssignee() {
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task saved = service().assign(sample(), "admin-1", "ORG01");

        assertEquals(TaskService.STATUS_TODO, saved.getStatus());
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertEquals("worker-1", captor.getValue().getUserId());
        assertEquals("TASK", captor.getValue().getType());
    }

    @Test
    void completeRecordsNoteAndTime() {
        Task t = sample();
        t.setId("t1");
        t.setStatus(TaskService.STATUS_TODO);
        when(taskRepository.findById("t1")).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task done = service().complete("t1", "已完成巡棚");
        assertEquals(TaskService.STATUS_DONE, done.getStatus());
        assertEquals("已完成巡棚", done.getDoneNote());
        assertTrue(TaskService.overdueMark(done, new Date()).isEmpty());
    }

    @Test
    void overdueMarkFlagsLateCompletionAndPendingOverdue() {
        Task t = sample();
        t.setId("t2");
        Date due = new Date(System.currentTimeMillis() - 1000);
        t.setDueTime(due);
        assertEquals("OVERDUE", TaskService.overdueMark(t, new Date()));

        t.setStatus(TaskService.STATUS_DONE);
        t.setDoneTime(new Date(System.currentTimeMillis() + 1000));
        assertEquals("LATE_DONE", TaskService.overdueMark(t, new Date()));
    }

    @Test
    void completeTwiceRejected() {
        Task t = sample();
        t.setId("t3");
        t.setStatus(TaskService.STATUS_DONE);
        when(taskRepository.findById("t3")).thenReturn(Optional.of(t));
        assertThrows(IllegalStateException.class, () -> service().complete("t3", null));
    }
}