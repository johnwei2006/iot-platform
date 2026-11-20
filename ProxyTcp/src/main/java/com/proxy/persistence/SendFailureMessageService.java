package com.proxy.persistence;

import java.util.List;

import com.proxy.persistence.model.SendFailureMessage;

/**
 * 下发失败消息存储接口。Mongo 与 H2 两种实现各自 {@code @Profile} 注入唯一 Bean。
 * 注：原路由层暴露的 queryAllSendFailureMessages 与 findAllSendFailureMessages 语义相同，
 * 此处统一为 findAllSendFailureMessages（H2 实现已对齐改名）。
 */
public interface SendFailureMessageService {
	List<SendFailureMessage> findAllSendFailureMessages();

	SendFailureMessage getSendFailureMessageByMessageId(String vehicleIdentifier, long messageId);

	int deleteSendFailureMessageByMessageId(String vehicleIdentifier, long messageId);

	boolean saveSendFailureMessage(SendFailureMessage msg);
}
