package com.proxy.persistence.mongodb;

import java.util.Date;
import java.util.List;

import com.proxy.persistence.mongodb.model.UplinkMessage;

public interface UplinkMessageRepositoryCustom {
	public UplinkMessage queryUplinkMessage(String deviceId, long messageId);
	
	public int saveUplinkMessage(com.proxy.persistence.model.UplinkMessage uplink);
	
	public int deleteUplinkMessage(String deviceId, long messageId);

	public int deleteUplinkMessageByVehicle(String deviceId);
	
	public int deleteUplinkMessageLaterThanTime(Date receivedTime);

	public List<UplinkMessage> queryAllUplinkMessages();
	
}
