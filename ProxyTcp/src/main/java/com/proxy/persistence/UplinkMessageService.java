package com.proxy.persistence;

import java.util.Date;
import java.util.List;

import com.proxy.persistence.model.UplinkMessage;

/**
 * 上行消息存储接口。Mongo 与 H2 两种实现各自 {@code @Profile} 注入唯一 Bean。
 */
public interface UplinkMessageService {
	UplinkMessage getUplinkMessageById(String identifier, long messageId);

	int saveUplinkMessage(UplinkMessage uplink);

	int deleteUplinkMessageStatus(String identifier, long messageId);

	int deleteUplinkMessageStatusByVehicle(String identifier);

	int deleteUplinkMessageLaterThanTime(Date receivedTime);

	List<UplinkMessage> queryAllUplinkMessages();
}
