package com.proxy.persistence.h2.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 网关 token 申请记录（H2） */
@Entity
@Table(name = "GatewayTokenRequestRecord")
public class GatewayTokenRequestRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Long id;

	@Column(name = "deviceId")
	private String deviceId;

	@Column(name = "ip")
	private String ip;

	@Column(name = "port")
	private int port;

	@Column(name = "requestTime")
	private Date requestTime;

	/** SUCCESS / UNKNOWN_DEVICE / INVALID_SECRET / ERROR */
	@Column(name = "result")
	private String result;

	/** 认证成功时的 token（hex），失败为 null */
	@Column(name = "tokenHex")
	private String tokenHex;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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
