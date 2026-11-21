package com.iot.platform.service;

import com.iot.platform.model.Device;
import com.iot.platform.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    /** 预共享密钥占位掩码，更新接口收到该值视为"未修改" */
    public static final String SECRET_MASK = "******";
    private static final int SECRET_MIN_LENGTH = 8;
    private static final int SECRET_MAX_LENGTH = 64;

    @Autowired
    private DeviceRepository deviceRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public List<Device> list() {
        return deviceRepository.findAll();
    }

    public Optional<Device> get(String id) {
        return deviceRepository.findById(id);
    }

    public Optional<Device> getByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }

    public Device create(Device d) {
        Date now = new Date();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        if (d.getStatus() == null) {
            d.setStatus("OFFLINE");
        }
        if (d.getAuthSecret() != null) {
            validateSecret(d.getAuthSecret());
        }
        return deviceRepository.save(d);
    }

    public Device update(String id, Device src) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("device not found: " + id));
        if (src.getName() != null) {
            d.setName(src.getName());
        }
        if (src.getNodeId() != null) {
            d.setNodeId(src.getNodeId());
        }
        if (src.getIdentifierType() != null) {
            d.setIdentifierType(src.getIdentifierType());
        }
        if (src.getStatus() != null) {
            d.setStatus(src.getStatus());
        }
        // 掩码值或空值不覆盖已有密钥；显式传入新密钥时校验后更新
        if (src.getAuthSecret() != null && !SECRET_MASK.equals(src.getAuthSecret())) {
            validateSecret(src.getAuthSecret());
            d.setAuthSecret(src.getAuthSecret().trim());
        }
        d.setUpdatedAt(new Date());
        return deviceRepository.save(d);
    }

    /**
     * 更新预共享密钥；secret 为空时自动生成 32 位十六进制随机密钥。
     * @return 更新后的设备（authSecret 为掩码）
     */
    public Device updateSecret(String id, String secret) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("device not found: " + id));
        String newSecret = (secret == null || secret.trim().isEmpty())
                ? generateSecret() : secret.trim();
        validateSecret(newSecret);
        d.setAuthSecret(newSecret);
        d.setUpdatedAt(new Date());
        deviceRepository.save(d);
        d.setAuthSecret(SECRET_MASK);
        return d;
    }

    private void validateSecret(String secret) {
        if (secret == null) {
            return;
        }
        String s = secret.trim();
        if (s.length() < SECRET_MIN_LENGTH || s.length() > SECRET_MAX_LENGTH) {
            throw new RuntimeException("authSecret length must be between "
                    + SECRET_MIN_LENGTH + " and " + SECRET_MAX_LENGTH);
        }
    }

    private String generateSecret() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(Character.toUpperCase(Character.forDigit(secureRandom.nextInt(16), 16)));
        }
        return sb.toString();
    }

    public void delete(String id) {
        deviceRepository.deleteById(id);
    }

    public void markOnline(String deviceId) {
        Device d = deviceRepository.findByDeviceId(deviceId).orElseGet(() -> {
            Device n = new Device();
            n.setDeviceId(deviceId);
            n.setCreatedAt(new Date());
            return n;
        });
        d.setStatus("ONLINE");
        d.setLastSeen(new Date());
        d.setUpdatedAt(new Date());
        deviceRepository.save(d);
    }
}
