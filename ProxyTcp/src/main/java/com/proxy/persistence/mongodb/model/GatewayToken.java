package com.proxy.persistence.mongodb.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** 网关认证 token（Mongo） */
@Document(collection = "gateway_token")
public class GatewayToken {

	@Id
	private ObjectId id;

	private String deviceId;

	/** token 十六进制字符串（20 字符 = 10 字节） */
	private String tokenHex;

	private Date issuedAt;

	/** 每个网关同一时刻仅一条 active=true */
	private boolean active;

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getTokenHex() {
		return tokenHex;
	}

	public void setTokenHex(String tokenHex) {
		this.tokenHex = tokenHex;
	}

	public Date getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(Date issuedAt) {
		this.issuedAt = issuedAt;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
