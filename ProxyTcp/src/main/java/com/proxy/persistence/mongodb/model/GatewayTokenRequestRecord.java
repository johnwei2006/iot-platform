package com.proxy.persistence.mongodb.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** 网关 token 申请记录（Mongo） */
@Document(collection = "gateway_token_request_record")
public class GatewayTokenRequestRecord {

	@Id
	private ObjectId id;

	private String deviceId;
	private String ip;
	private int port;
	private Date requestTime;
	/** SUCCESS / UNKNOWN_DEVICE / INVALID_SECRET / ERROR */
	private String result;
	/** 认证成功时的 token（hex），失败为 null */
	private String tokenHex;

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

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Date getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getTokenHex() {
		return tokenHex;
	}

	public void setTokenHex(String tokenHex) {
		this.tokenHex = tokenHex;
	}
}
