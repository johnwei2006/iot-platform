package com.proxy.persistence;

import java.util.List;

import com.proxy.persistence.model.GatewayReconnectRecord;
import com.proxy.persistence.model.GatewayTokenRequestRecord;

/**
 * 网关重连记录 / token 申请记录存储接口。
 */
public interface GatewayRecordService {

	void saveReconnectRecord(String deviceId, String ip, int port, String type);

	void saveTokenRequestRecord(String deviceId, String ip, int port, String result, String tokenHex);

	/** deviceId 为 null 时查询全部；按时间倒序分页 */
	List<GatewayReconnectRecord> queryReconnectRecords(String deviceId, int page, int size);

	List<GatewayTokenRequestRecord> queryTokenRequestRecords(String deviceId, int page, int size);

	long countReconnectRecords(String deviceId);

	long countTokenRequestRecords(String deviceId);
}
