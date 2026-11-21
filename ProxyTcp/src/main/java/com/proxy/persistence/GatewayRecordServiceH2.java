package com.proxy.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.proxy.persistence.model.GatewayReconnectRecord;
import com.proxy.persistence.model.GatewayTokenRequestRecord;
import com.proxy.persistence.h2.GatewayReconnectRecordRespository;
import com.proxy.persistence.h2.GatewayTokenRequestRecordRespository;

/**
 * H2 模式网关重连记录 / token 申请记录存储。
 * H2 实体与 persistence.model 服务层模型同名，实体侧使用全限定名访问。
 */
@Service
@org.springframework.context.annotation.Profile("h2")
public class GatewayRecordServiceH2 implements GatewayRecordService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	@Autowired
	private GatewayReconnectRecordRespository gatewayReconnectRecordRespository;

	@Autowired
	private GatewayTokenRequestRecordRespository gatewayTokenRequestRecordRespository;

	@Override
	public void saveReconnectRecord(String deviceId, String ip, int port, String type) {
		com.proxy.persistence.h2.model.GatewayReconnectRecord record =
				new com.proxy.persistence.h2.model.GatewayReconnectRecord();
		record.setDeviceId(deviceId);
		record.setIp(ip);
		record.setPort(port);
		record.setConnectTime(new Date());
		record.setType(type);
		gatewayReconnectRecordRespository.save(record);
	}

	@Override
	public void saveTokenRequestRecord(String deviceId, String ip, int port, String result, String tokenHex) {
		com.proxy.persistence.h2.model.GatewayTokenRequestRecord record =
				new com.proxy.persistence.h2.model.GatewayTokenRequestRecord();
		record.setDeviceId(deviceId);
		record.setIp(ip);
		record.setPort(port);
		record.setRequestTime(new Date());
		record.setResult(result);
		record.setTokenHex(tokenHex);
		gatewayTokenRequestRecordRespository.save(record);
	}

	@Override
	public List<GatewayReconnectRecord> queryReconnectRecords(String deviceId, int page, int size) {
		int pageIndex = Math.max(page, 0);
		int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 200);
		PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
		List<com.proxy.persistence.h2.model.GatewayReconnectRecord> records;
		if (deviceId == null || deviceId.isEmpty()) {
			records = (List<com.proxy.persistence.h2.model.GatewayReconnectRecord>)
					gatewayReconnectRecordRespository.findAll(pageRequest);
		} else {
			records = gatewayReconnectRecordRespository.findByDeviceIdOrderByConnectTimeDesc(deviceId, pageRequest);
		}
		List<GatewayReconnectRecord> result = new ArrayList<GatewayReconnectRecord>();
		for (com.proxy.persistence.h2.model.GatewayReconnectRecord record : records) {
			result.add(toModel(record));
		}
		return result;
	}

	@Override
	public List<GatewayTokenRequestRecord> queryTokenRequestRecords(String deviceId, int page, int size) {
		int pageIndex = Math.max(page, 0);
		int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 200);
		PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
		List<com.proxy.persistence.h2.model.GatewayTokenRequestRecord> records;
		if (deviceId == null || deviceId.isEmpty()) {
			records = (List<com.proxy.persistence.h2.model.GatewayTokenRequestRecord>)
					gatewayTokenRequestRecordRespository.findAll(pageRequest);
		} else {
			records = gatewayTokenRequestRecordRespository.findByDeviceIdOrderByRequestTimeDesc(deviceId, pageRequest);
		}
		List<GatewayTokenRequestRecord> result = new ArrayList<GatewayTokenRequestRecord>();
		for (com.proxy.persistence.h2.model.GatewayTokenRequestRecord record : records) {
			result.add(toModel(record));
		}
		return result;
	}

	@Override
	public long countReconnectRecords(String deviceId) {
		if (deviceId == null || deviceId.isEmpty()) {
			return gatewayReconnectRecordRespository.count();
		}
		return gatewayReconnectRecordRespository.countByDeviceId(deviceId);
	}

	@Override
	public long countTokenRequestRecords(String deviceId) {
		if (deviceId == null || deviceId.isEmpty()) {
			return gatewayTokenRequestRecordRespository.count();
		}
		return gatewayTokenRequestRecordRespository.countByDeviceId(deviceId);
	}

	private GatewayReconnectRecord toModel(com.proxy.persistence.h2.model.GatewayReconnectRecord record) {
		GatewayReconnectRecord model = new GatewayReconnectRecord();
		model.setDeviceId(record.getDeviceId());
		model.setIp(record.getIp());
		model.setPort(record.getPort());
		model.setConnectTime(record.getConnectTime());
		model.setType(record.getType());
		return model;
	}

	private GatewayTokenRequestRecord toModel(com.proxy.persistence.h2.model.GatewayTokenRequestRecord record) {
		GatewayTokenRequestRecord model = new GatewayTokenRequestRecord();
		model.setDeviceId(record.getDeviceId());
		model.setIp(record.getIp());
		model.setPort(record.getPort());
		model.setRequestTime(record.getRequestTime());
		model.setResult(record.getResult());
		model.setTokenHex(record.getTokenHex());
		return model;
	}
}
