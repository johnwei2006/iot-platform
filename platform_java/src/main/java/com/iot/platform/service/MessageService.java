package com.iot.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.Message;
import com.iot.platform.model.NotifyPref;
import com.iot.platform.repository.MessageRepository;
import com.iot.platform.repository.NotifyPrefRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 站内消息：列表筛选（待办/未读/全部）、已读、未读角标、通知偏好存取。 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final NotifyPrefRepository prefRepository;

    public MessageService(MessageRepository messageRepository, NotifyPrefRepository prefRepository) {
        this.messageRepository = messageRepository;
        this.prefRepository = prefRepository;
    }

    public List<Message> listByUser(String userId, String type, Boolean unread) {
        var wrapper = Wrappers.<Message>lambdaQuery().eq(Message::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Message::getType, type);
        }
        if (Boolean.TRUE.equals(unread)) {
            wrapper.isNull(Message::getReadAt);
        }
        wrapper.orderByDesc(Message::getCreatedAt);
        return messageRepository.findAll(wrapper);
    }

    public long unreadCount(String userId) {
        return messageRepository.count(Wrappers.<Message>lambdaQuery()
                .eq(Message::getUserId, userId)
                .isNull(Message::getReadAt));
    }

    public Message markRead(String id, String userId) {
        Message m = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        if (!userId.equals(m.getUserId())) {
            throw new IllegalStateException("无权操作该消息");
        }
        if (m.getReadAt() == null) {
            m.setReadAt(new Date());
            messageRepository.save(m);
        }
        return m;
    }

    /** 通知偏好：不存在则建默认（全类型 + 仅站内）。 */
    public NotifyPref prefOf(String userId) {
        List<NotifyPref> existing = prefRepository.findAll(Wrappers.<NotifyPref>lambdaQuery()
                .eq(NotifyPref::getUserId, userId));
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        NotifyPref pref = new NotifyPref();
        pref.setId(UUID.randomUUID().toString());
        pref.setUserId(userId);
        pref.setReceiveKinds("[\"TASK\",\"ALARM\",\"SYSTEM\"]");
        pref.setChannels("[\"INAPP\"]");
        pref.setUpdatedAt(new Date());
        return prefRepository.save(pref);
    }

    public NotifyPref updatePref(String userId, String receiveKinds, String channels) {
        NotifyPref pref = prefOf(userId);
        if (receiveKinds != null) {
            pref.setReceiveKinds(receiveKinds);
        }
        if (channels != null) {
            pref.setChannels(channels);
        }
        pref.setUpdatedAt(new Date());
        return prefRepository.save(pref);
    }
}