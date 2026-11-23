package com.proxy.persistence;

import java.util.Date;
import java.util.List;

import com.proxy.app.Constants.Status;
import com.proxy.persistence.model.DownlinkMessage;

/**
 * 下行消息存储接口。Mongo 与 H2 两种实现各自 {@code @Profile} 注入唯一 Bean，
 * 由 Spring 按激活 profile 解析，调用方无需关心底层存储（原路由层 if/else 已去除）。
 */
public interface DownlinkMessageService {
	List<DownlinkMessage> findAllDownlinkMessages();

	List<DownlinkMessage> findAllDownlinkMessagesExceptStatus(Status p_status);

	List<DownlinkMessage> findAllDownlinkMessagesExceptStatus(Status p_status, Date since);

	int failExpiredDownlinkMessages(Date expireBefore);

	List<DownlinkMessage> findVehicleDownlinkMessagesExceptStatus(String vin, Status p_status);

	List<DownlinkMessage> findAllDownlinkMessagesWithStatus(Status p_status);

	DownlinkMessage getDownlinkMessageByMessageId(String vehicleIdentifier, long messageId);

	DownlinkMessage updateDownlinkMessage(DownlinkMessage downlinkSendStatus);

	int deleteDownlinkMessageByMessageId(String vehicleIdentifier, long messageId);

	int deleteAllAckDownlinkMessage();

	List<DownlinkMessage> queryAllNotAckRecords();

	int deleteOverResendMessages(int resendCount);
}
