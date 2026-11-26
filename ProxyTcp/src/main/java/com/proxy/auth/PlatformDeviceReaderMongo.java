package com.proxy.auth;

import java.util.Date;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

/**
 * Mongo 模式网关凭据读写：使用 ProxyTcp 自身 proxy 库的 gateway_credential 集合。
 *
 * <p>凭据由 ProxyTcp 的运维接口 POST /api/gateway/credentials 录入并维护，
 * 不再依赖已废弃的 iot_platform.devices 集合（platform_java 已迁 MySQL）。
 */
@Service
@Profile("mongo")
public class PlatformDeviceReaderMongo implements PlatformDeviceReader {

	private static final String CREDENTIAL_COLLECTION = "gateway_credential";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public String getAuthSecret(String deviceId) {
		try {
			Query query = Query.query(Criteria.where("deviceId").is(deviceId));
			Document credential = mongoTemplate.findOne(query, Document.class, CREDENTIAL_COLLECTION);
			if (credential == null) {
				return null;
			}
			Object secret = credential.get("authSecret");
			return secret == null ? null : secret.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read gateway credential from proxy.gateway_credential: "
					+ e.getMessage(), e);
		}
	}

	@Override
	public boolean upsertAuthSecret(String deviceId, String secret) {
		try {
			Query query = Query.query(Criteria.where("deviceId").is(deviceId));
			Document credential = mongoTemplate.findOne(query, Document.class, CREDENTIAL_COLLECTION);
			Date now = new Date();
			if (credential == null) {
				Document newCredential = new Document();
				newCredential.put("deviceId", deviceId);
				newCredential.put("authSecret", secret);
				newCredential.put("createdAt", now);
				newCredential.put("updatedAt", now);
				mongoTemplate.insert(newCredential, CREDENTIAL_COLLECTION);
			} else {
				Update update = new Update();
				update.set("authSecret", secret);
				update.set("updatedAt", now);
				mongoTemplate.updateFirst(query, update, CREDENTIAL_COLLECTION);
			}
			return true;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to upsert gateway credential: " + e.getMessage(), e);
		}
	}
}
