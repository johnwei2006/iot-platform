package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.model.Node;
import com.iot.platform.repository.DeviceCtrlRepository;
import com.iot.platform.repository.NodeRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import com.iot.platform.service.control.GatewayEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** 控制设备服务：节点下挂靠的第三层受控实体。 */
@Service
public class DeviceCtrlService {

    @Autowired
    private DeviceCtrlRepository deviceCtrlRepository;

    @Autowired
    private NodeRepository nodeRepository;

    public List<DeviceCtrl> list() {
        return deviceCtrlRepository.findAll();
    }

    public List<DeviceCtrl> listByCriteria(DataScopeQuery scope) {
        if (scope == null || scope.getType() == DataScopeQuery.Type.ALL) {
            return deviceCtrlRepository.findAll();
        }
        if (scope.getType() == DataScopeQuery.Type.DENY_ALL) {
            return List.of();
        }
        LambdaQueryWrapper<DeviceCtrl> wrapper = Wrappers.<DeviceCtrl>lambdaQuery().eq(DeviceCtrl::getIsDeleted, 0);
        switch (scope.getType()) {
            case ORG_AND_CHILDREN -> wrapper.likeRight(DeviceCtrl::getOrgCode, scope.getValue());
            case ORG_ONLY -> wrapper.eq(DeviceCtrl::getOrgCode, scope.getValue());
            case SELF -> wrapper.eq(DeviceCtrl::getOwnerUserId, scope.getValue());
            default -> {
                return List.of();
            }
        }
        return deviceCtrlRepository.findAll(wrapper);
    }

    public List<DeviceCtrl> listByNodeCode(String nodeCode) {
        return deviceCtrlRepository.findByNodeCode(nodeCode);
    }

    public Optional<DeviceCtrl> get(String id) {
        return deviceCtrlRepository.findById(id);
    }

    public Optional<DeviceCtrl> getByDeviceCode(String deviceCode) {
        return deviceCtrlRepository.findByDeviceCode(deviceCode);
    }

    public DeviceCtrl create(DeviceCtrl d, String subject, String creatorOrgCode) {
        Date now = new Date();
        d.setCreateTime(now);
        d.setUpdateTime(now);
        d.setCreateBy(subject);
        d.setUpdateBy(subject);
        if (d.getIsDeleted() == null) {
            d.setIsDeleted(0);
        }
        if (d.getOnline() == null) {
            d.setOnline(1);
        }
        if (d.getAuto() == null) {
            d.setAuto(1);
        }
        if (d.getOrgCode() == null || d.getOrgCode().trim().isEmpty()) {
            d.setOrgCode(creatorOrgCode);
        }
        d.setOwnerUserId(subject);
        return deviceCtrlRepository.save(d);
    }

    public DeviceCtrl update(String id, DeviceCtrl src) {
        DeviceCtrl d = deviceCtrlRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("device not found: " + id));
        if (src.getDeviceName() != null) d.setDeviceName(src.getDeviceName());
        if (src.getDeviceClass() != null) d.setDeviceClass(src.getDeviceClass());
        if (src.getDeviceClassType() != null) d.setDeviceClassType(src.getDeviceClassType());
        if (src.getNodeCode() != null) d.setNodeCode(src.getNodeCode());
        if (src.getStatus() != null) d.setStatus(src.getStatus());
        if (src.getOnline() != null) d.setOnline(src.getOnline());
        if (src.getAuto() != null) d.setAuto(src.getAuto());
        if (src.getAddress() != null) d.setAddress(src.getAddress());
        if (src.getLongitude() != null) d.setLongitude(src.getLongitude());
        if (src.getLatitude() != null) d.setLatitude(src.getLatitude());
        if (src.getPic() != null) d.setPic(src.getPic());
        if (src.getChannel() != null) d.setChannel(src.getChannel());
        if (src.getConnectType() != null) d.setConnectType(src.getConnectType());
        d.setUpdateBy(src.getUpdateBy());
        d.setUpdateTime(new Date());
        return deviceCtrlRepository.save(d);
    }

    public void delete(String id) {
        deviceCtrlRepository.deleteById(id);
    }

    /**
     * 控制设备 -> 下发三元组映射（GatewayEntry）：
     * deviceId = 节点挂靠的网关编码（node.gateway_code），nodeId = 节点编码，
     * channel = device.channel（非数字通道返回 null，由校验层拒绝）。
     */
    public GatewayEntry toGatewayEntry(DeviceCtrl d) {
        if (d.getNodeCode() == null || d.getNodeCode().isEmpty()) {
            throw new IllegalStateException("控制设备未挂靠节点: " + d.getDeviceCode());
        }
        Node node = nodeRepository.findByNodeCode(d.getNodeCode())
                .orElseThrow(() -> new IllegalStateException(
                        "节点不存在: " + d.getNodeCode() + " (device=" + d.getDeviceCode() + ")"));
        Integer channel = null;
        if (d.getChannel() != null) {
            try {
                channel = Integer.valueOf(d.getChannel().trim());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("控制设备通道号不是数字: " + d.getDeviceCode()
                        + " channel=" + d.getChannel());
            }
        }
        return new GatewayEntry(node.getGatewayCode(), node.getNodeCode(), channel,
                d.getDeviceName(), d.getDeviceClass());
    }

    /** 按设备编码映射为下发三元组。 */
    public GatewayEntry toGatewayEntryByDeviceCode(String deviceCode) {
        DeviceCtrl d = deviceCtrlRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new IllegalStateException("控制设备不存在: " + deviceCode));
        return toGatewayEntry(d);
    }
}
