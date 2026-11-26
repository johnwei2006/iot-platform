package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.Gateway;
import com.iot.platform.repository.GatewayRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class GatewayService {

    /** 预共享密钥占位掩码，更新接口收到该值视为"未修改" */
    public static final String SECRET_MASK = "******";
    private static final int SECRET_MIN_LENGTH = 8;
    private static final int SECRET_MAX_LENGTH = 64;

    @Autowired
    private GatewayRepository deviceRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    /** 全量列表（内部/网关路径使用）。 */
    public List<Gateway> list() {
        return deviceRepository.findAll();
    }

    /** 按数据权限条件过滤列表；scope 为 null 表示不过滤（ALL）。 */
    public List<Gateway> listByCriteria(DataScopeQuery scope) {
        if (scope == null || scope.getType() == DataScopeQuery.Type.ALL) {
            return deviceRepository.findAll();
        }
        if (scope.getType() == DataScopeQuery.Type.DENY_ALL) {
            return List.of();
        }
        LambdaQueryWrapper<Gateway> wrapper = Wrappers.<Gateway>lambdaQuery();
        switch (scope.getType()) {
            case ORG_AND_CHILDREN -> wrapper.likeRight(Gateway::getOrgCode, scope.getValue());
            case ORG_ONLY -> wrapper.eq(Gateway::getOrgCode, scope.getValue());
            case SELF -> wrapper.eq(Gateway::getOwnerUserId, scope.getValue());
            default -> {
                return List.of();
            }
        }
        return deviceRepository.findAll(wrapper);
    }

    public Optional<Gateway> get(String id) {
        return deviceRepository.findById(id);
    }

    public Optional<Gateway> getByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }

    /** 创建设备并绑定归属（ownerUserId=创建者；orgCode 未指定时回落创建者机构）。 */
    public Gateway create(Gateway d, String subject, String creatorOrgCode) {
        Date now = new Date();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        if (d.getStatus() == null) {
            d.setStatus("OFFLINE");
        }
        if (d.getOrgCode() == null || d.getOrgCode().trim().isEmpty()) {
            d.setOrgCode(creatorOrgCode);
        }
        d.setOwnerUserId(subject);
        if (d.getAuthSecret() != null) {
            validateSecret(d.getAuthSecret());
        }
        return deviceRepository.save(d);
    }

    public Gateway update(String id, Gateway src) {
        Gateway d = deviceRepository.findById(id)
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
        if (src.getOrgCode() != null) {
            d.setOrgCode(src.getOrgCode());
        }
        if (src.getAddress() != null) {
            d.setAddress(src.getAddress());
        }
        if (src.getLongitude() != null) {
            d.setLongitude(src.getLongitude());
        }
        if (src.getLatitude() != null) {
            d.setLatitude(src.getLatitude());
        }
        if (src.getGatewayType() != null) {
            d.setGatewayType(src.getGatewayType());
        }
        if (src.getFirmwareVersion() != null) {
            d.setFirmwareVersion(src.getFirmwareVersion());
        }
        if (src.getPic() != null) {
            d.setPic(src.getPic());
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
    public Gateway updateSecret(String id, String secret) {
        Gateway d = deviceRepository.findById(id)
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
        Gateway d = deviceRepository.findByDeviceId(deviceId).orElseGet(() -> {
            Gateway n = new Gateway();
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
