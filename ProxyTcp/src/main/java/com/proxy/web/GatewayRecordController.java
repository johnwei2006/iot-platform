package com.proxy.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.proxy.auth.PlatformDeviceReader;
import com.proxy.persistence.GatewayRecordService;

/**
 * 网关认证记录查询接口（供 web_vue3 管理端使用）。
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayRecordController {

	@Autowired
	private GatewayRecordService gatewayRecordService;

	@Autowired
	private PlatformDeviceReader platformDeviceReader;

	/** 网关连接/重连记录分页查询 */
	@GetMapping("/reconnect-records")
	public Map<String, Object> reconnectRecords(
			@RequestParam(required = false) String deviceId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("total", gatewayRecordService.countReconnectRecords(deviceId));
		response.put("page", page);
		response.put("size", size);
		response.put("records", gatewayRecordService.queryReconnectRecords(deviceId, page, size));
		return response;
	}

	/** 网关 token 申请记录分页查询 */
	@GetMapping("/token-requests")
	public Map<String, Object> tokenRequests(
			@RequestParam(required = false) String deviceId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("total", gatewayRecordService.countTokenRequestRecords(deviceId));
		response.put("page", page);
		response.put("size", size);
		response.put("records", gatewayRecordService.queryTokenRequestRecords(deviceId, page, size));
		return response;
	}

	/** 录入/更新网关预共享密钥（h2 模式的运维入口；mongo 模式由 platform_java 设备管理） */
	@PostMapping("/credentials")
	public Map<String, Object> upsertCredential(@RequestBody Map<String, String> body) {
		Map<String, Object> response = new HashMap<String, Object>();
		String deviceId = body == null ? null : body.get("deviceId");
		String secret = body == null ? null : body.get("authSecret");
		if (deviceId == null || deviceId.trim().isEmpty() || secret == null || secret.trim().isEmpty()) {
			response.put("success", false);
			response.put("message", "deviceId and authSecret are required");
			return response;
		}
		try {
			platformDeviceReader.upsertAuthSecret(deviceId.trim(), secret.trim());
			response.put("success", true);
			response.put("message", "credential saved");
		} catch (Exception e) {
			response.put("success", false);
			response.put("message", e.getMessage());
		}
		return response;
	}
}
