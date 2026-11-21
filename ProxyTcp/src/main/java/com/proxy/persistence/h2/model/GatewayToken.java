package com.proxy.persistence.h2.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 网关认证 token（H2） */
@Entity
@Table(name = "GatewayToken")
public class GatewayToken {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Long id;

	@Column(name = "deviceId")
	private String deviceId;

	/** token 十六进制字符串（20 字符 = 10 字节） */
	@Column(name = "tokenHex")
	private String tokenHex;

	@Column(name = "issuedAt")
	private Date issuedAt;

	@Column(name = "active")
	private boolean active;

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
