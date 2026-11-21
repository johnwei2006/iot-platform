package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.Device;
import com.iot.platform.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @GetMapping
    public ApiResponse<?> list() {
        List<Device> devices = deviceService.list();
        devices.forEach(this::maskSecret);
        return ApiResponse.success(devices);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id) {
        Optional<Device> d = deviceService.get(id);
        if (d.isPresent()) {
            maskSecret(d.get());
            return ApiResponse.success(d.get());
        }
        return ApiResponse.fail(ResultCode.NOT_FOUND);
    }

    /** 更新网关认证预共享密钥；body 可为空（自动生成随机密钥）或 {"authSecret":"xxx"} */
    @PutMapping("/{id}/secret")
    public ApiResponse<?> updateSecret(@PathVariable String id,
                                       @RequestBody(required = false) Map<String, String> body) {
        try {
            String secret = body == null ? null : body.get("authSecret");
            return ApiResponse.success(deviceService.updateSecret(id, secret));
        } catch (RuntimeException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    private void maskSecret(Device d) {
        if (d.getAuthSecret() != null && !d.getAuthSecret().isEmpty()) {
            d.setAuthSecret(DeviceService.SECRET_MASK);
        }
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Device d) {
        return ApiResponse.success(deviceService.create(d));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Device d) {
        try {
            return ApiResponse.success(deviceService.update(id, d));
        } catch (RuntimeException e) {
            return ApiResponse.fail(404, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        deviceService.delete(id);
        return ApiResponse.success(null);
    }
}
