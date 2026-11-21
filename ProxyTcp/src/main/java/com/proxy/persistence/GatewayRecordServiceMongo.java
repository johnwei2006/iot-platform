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
import com.proxy.persistence.mongodb.GatewayReconnectRecordRepository;
import com.proxy.persistence.mongodb.GatewayTokenRequestRecordRepository;

/**
 * Mongo 模式网关重连记录 / token 申请记录存储。
 * Mongo 实体与 persistence.model 服务层模型同名，实体侧使用全限定名访问。
 */
@Service
@org.springframework.context.annotation.Profile("mongo")
public class GatewayRecordServiceMongo implements GatewayRecordService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	@Autowired
	private GatewayReconnectRecordRepository gatewayReconnectRecordRepository;

	@Autowired
	private GatewayTokenRequestRecordRepository gatewayTokenRequestRecordRepository;

	@Override
	public void saveReconnectRecord(String deviceId, String ip, int port, String type) {
		com.proxy.persistence.mongodb.model.GatewayReconnectRecord record =
				new com.proxy.persistence.mongodb.model.GatewayReconnectRecord();
		record.setDeviceId(deviceId);
		record.setIp(ip);
		record.setPort(port);
		record.setConnectTime(new Date());
		record.setType(type);
		gatewayReconnectRecordRepository.save(record);
	}

	@Override
	public void saveTokenRequestRecord(String deviceId, String ip, int port, String result, String tokenHex) {
		com.proxy.persistence.mongodb.model.GatewayTokenRequestRecord record =
				new com.proxy.persistence.mongodb.model.GatewayTokenRequestRecord();
		record.setDeviceId(deviceId);
		record.setIp(ip);
		record.setPort(port);
		record.setRequestTime(new Date());
		record.setResult(result);
		record.setTokenHex(tokenHex);
		gatewayTokenRequestRecordRepository.save(record);
	}

	@Override
	public List<GatewayReconnectRecord> queryReconnectRecords(String deviceId, int page, int size) {
		int pageIndex = Math.max(page, 0);
		int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 200);
		PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
		List<com.proxy.persistence.mongodb.model.GatewayReconnectRecord> records;
		if (deviceId == null || deviceId.isEmpty()) {
			records = gatewayReconnectRecordRepository.findAll(pageRequest).getContent();
		} else {
			records = gatewayReconnectRecordRepository.findByDeviceIdOrderByConnectTimeDesc(deviceId, pageRequest);
		}
		List<GatewayReconnectRecord> result = new ArrayList<GatewayReconnectRecord>();
		for (com.proxy.persistence.mongodb.model.GatewayReconnectRecord record : records) {
			result.add(toModel(record));
		}
		return result;
	}

	@Override
	public List<GatewayTokenRequestRecord> queryTokenRequestRecords(String deviceId, int page, int size) {
		int pageIndex = Math.max(page, 0);
		int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 200);
		PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
		List<com.proxy.persistence.mongodb.model.GatewayTokenRequestRecord> records;
		if (deviceId == null || deviceId.isEmpty()) {
			records = gatewayTokenRequestRecordRepository.findAll(pageRequest).getContent();
		} else {
			records = gatewayTokenRequestRecordRepository.findByDeviceIdOrderByRequestTimeDesc(deviceId, pageRequest);
		}
		List<GatewayTokenRequestRecord> result = new ArrayList<GatewayTokenRequestRecord>();
		for (com.proxy.persistence.mongodb.model.GatewayTokenRequestRecord record : records) {
			result.add(toModel(record));
		}
		return result;
	}

	@Override
	public long countReconnectRecords(String deviceId) {
		if (deviceId == null || deviceId.isEmpty()) {
			return gatewayReconnectRecordRepository.count();
		}
		return gatewayReconnectRecordRepository.countByDeviceId(deviceId);
	}

	@Override
	public long countTokenRequestRecords(String deviceId) {
		if (deviceId == null || deviceId.isEmpty()) {
			return gatewayTokenRequestRecordRepository.count();
		}
		return gatewayTokenRequestRecordRepository.countByDeviceId(deviceId);
	}

	private GatewayReconnectRecord toModel(com.proxy.persistence.mongodb.model.GatewayReconnectRecord record) {
		GatewayReconnectRecord model = new GatewayReconnectRecord();
		model.setDeviceId(record.getDeviceId());
		model.setIp(record.getIp());
		model.setPort(record.getPort());
		model.setConnectTime(record.getConnectTime());
		model.setType(record.getType());
		return model;
	}

	private GatewayTokenRequestRecord toModel(com.proxy.persistence.mongodb.model.GatewayTokenRequestRecord record) {
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
