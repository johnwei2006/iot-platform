package com.proxy.persistence.mongodb;

import java.util.Date;
import java.util.List;

import com.proxy.Constants.Status;
import com.proxy.persistence.mongodb.model.DownlinkMessage;

public interface DownlinkMessageRepositoryCustom{
	public DownlinkMessage getDownlinkMessageByMessageId(String vehicleIdentifier, long messageId);
	public long updateDownlinkMessage(DownlinkMessage downlinkSendStatus);
	public int  deleteDownlinkMessageByMessageId(String vehicleIdentifier, long messageId);
	public int deleteAllAckDownlinkMessage(Status status);
	public List<DownlinkMessage> queryOverResendMessages(int count);
	public List<DownlinkMessage> queryAllNotAckRecords(Status status);
	public List<DownlinkMessage> queryAllDownlinkMessages();
	public List<DownlinkMessage> queryAllDownlinkMessagesExceptStatus(Status p_status);
	public List<DownlinkMessage> queryAllDownlinkMessagesExceptStatusSince(Status p_status, Date since);
	public int updateExpiredDownlinkMessagesFailed(Date expireBefore);
	public List<DownlinkMessage> queryVehicleDownlinkMessagesExceptStatus(String vin, Status p_status);
	public List<DownlinkMessage> queryAllDownlinkMessagesWithStatus(Status p_status);
	public int deleteOverResendMessages(int resendCount);
}
