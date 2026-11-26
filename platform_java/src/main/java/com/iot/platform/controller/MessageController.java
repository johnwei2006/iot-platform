package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 站内消息：列表筛选（类型/未读）、未读角标、标记已读。仅站内，不实际推送。 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String type,
                               @RequestParam(required = false) Boolean unread,
                               HttpServletRequest request) {
        String userId = (String) request.getAttribute("subject");
        return ApiResponse.success(messageService.listByUser(userId, type, unread));
    }

    @GetMapping("/unread-count")
    public ApiResponse<?> unreadCount(HttpServletRequest request) {
        String userId = (String) request.getAttribute("subject");
        return ApiResponse.success(Map.of("count", messageService.unreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<?> markRead(@PathVariable String id, HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("subject");
            return ApiResponse.success(messageService.markRead(id, userId));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @GetMapping("/notify-prefs")
    public ApiResponse<?> pref(HttpServletRequest request) {
        String userId = (String) request.getAttribute("subject");
        return ApiResponse.success(messageService.prefOf(userId));
    }

    /** 更新通知偏好：body {"receiveKinds":"[\"ALARM\"]","channels":"[\"INAPP\"]"}（JSON 字符串）。 */
    @PostMapping("/notify-prefs")
    public ApiResponse<?> updatePref(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String userId = (String) request.getAttribute("subject");
        return ApiResponse.success(messageService.updatePref(userId,
                body.get("receiveKinds"), body.get("channels")));
    }
}