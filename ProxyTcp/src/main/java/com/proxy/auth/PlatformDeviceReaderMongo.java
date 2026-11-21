package com.proxy.auth;

import java.util.Date;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

/**
 * Mongo 模式网关凭据读取：直接访问 platform_java 维护的 iot_platform.devices 集合。
 */
@Service
@Profile("mongo")
public class PlatformDeviceReaderMongo implements PlatformDeviceReader {

	private static final String DEVICES_COLLECTION = "devices";

	@Autowired
	@Qualifier("platformMongoTemplate")
	private MongoTemplate platformMongoTemplate;

	@Override
	public String getAuthSecret(String deviceId) {
		try {
			Query query = Query.query(Criteria.where("deviceId").is(deviceId));
			Document device = platformMongoTemplate.findOne(query, Document.class, DEVICES_COLLECTION);
			if (device == null) {
				return null;
			}
			Object secret = device.get("authSecret");
			return secret == null ? null : secret.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read gateway credential from platform devices: "
					+ e.getMessage(), e);
		}
	}

	@Override
	public boolean upsertAuthSecret(String deviceId, String secret) {
		try {
			Query query = Query.query(Criteria.where("deviceId").is(deviceId));
			Document device = platformMongoTemplate.findOne(query, Document.class, DEVICES_COLLECTION);
			Date now = new Date();
			if (device == null) {
				Document newDevice = new Document();
				newDevice.put("deviceId", deviceId);
				newDevice.put("name", deviceId);
				newDevice.put("authSecret", secret);
				newDevice.put("status", "OFFLINE");
				newDevice.put("createdAt", now);
				newDevice.put("updatedAt", now);
				platformMongoTemplate.insert(newDevice, DEVICES_COLLECTION);
			} else {
				Update update = new Update();
				update.set("authSecret", secret);
				update.set("updatedAt", now);
				platformMongoTemplate.updateFirst(query, update, DEVICES_COLLECTION);
			}
			return true;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to upsert gateway credential: " + e.getMessage(), e);
		}
	}
}
