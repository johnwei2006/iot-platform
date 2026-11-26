package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.Node;
import com.iot.platform.repository.NodeRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** 节点服务：网关下挂靠的第二层实体。 */
@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    /** 全量列表（内部链路使用）。 */
    public List<Node> list() {
        return nodeRepository.findAll();
    }

    /** 按数据权限条件过滤列表；scope 为 null 表示不过滤（ALL）。 */
    public List<Node> listByCriteria(DataScopeQuery scope) {
        return listByCriteria(scope, null);
    }

    /** 按数据权限与安装区域过滤列表；areaId 为空时不限制区域。 */
    public List<Node> listByCriteria(DataScopeQuery scope, String areaId) {
        boolean hasArea = areaId != null && !areaId.trim().isEmpty();
        if (scope == null || scope.getType() == DataScopeQuery.Type.ALL) {
            if (!hasArea) {
                return nodeRepository.findAll();
            }
            return nodeRepository.findAll(Wrappers.<Node>lambdaQuery()
                    .eq(Node::getIsDeleted, 0).eq(Node::getAreaId, areaId.trim()));
        }
        if (scope.getType() == DataScopeQuery.Type.DENY_ALL) {
            return List.of();
        }
        LambdaQueryWrapper<Node> wrapper = Wrappers.<Node>lambdaQuery().eq(Node::getIsDeleted, 0);
        switch (scope.getType()) {
            case ORG_AND_CHILDREN -> wrapper.likeRight(Node::getOrgCode, scope.getValue());
            case ORG_ONLY -> wrapper.eq(Node::getOrgCode, scope.getValue());
            case SELF -> wrapper.eq(Node::getOwnerUserId, scope.getValue());
            default -> {
                return List.of();
            }
        }
        if (hasArea) {
            wrapper.eq(Node::getAreaId, areaId.trim());
        }
        return nodeRepository.findAll(wrapper);
    }

    /** 按网关编码查节点列表。 */
    public List<Node> listByGatewayCode(String gatewayCode) {
        return nodeRepository.findAll(Wrappers.<Node>lambdaQuery()
                .eq(Node::getGatewayCode, gatewayCode).eq(Node::getIsDeleted, 0));
    }

    public Optional<Node> get(String id) {
        return nodeRepository.findById(id);
    }

    public Optional<Node> getByNodeCode(String nodeCode) {
        return nodeRepository.findByNodeCode(nodeCode);
    }

    /** 创建节点并绑定归属；orgCode 未指定时回落创建者机构。 */
    public Node create(Node n, String subject, String creatorOrgCode) {
        Date now = new Date();
        n.setCreateTime(now);
        n.setUpdateTime(now);
        n.setCreateBy(subject);
        n.setUpdateBy(subject);
        if (n.getIsDeleted() == null) {
            n.setIsDeleted(0);
        }
        if (n.getOnline() == null) {
            n.setOnline(1);
        }
        if (n.getOrgCode() == null || n.getOrgCode().trim().isEmpty()) {
            n.setOrgCode(creatorOrgCode);
        }
        n.setOwnerUserId(subject);
        return nodeRepository.save(n);
    }

    public Node update(String id, Node src) {
        Node n = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("node not found: " + id));
        if (src.getNodeName() != null) n.setNodeName(src.getNodeName());
        if (src.getGatewayCode() != null) n.setGatewayCode(src.getGatewayCode());
        if (src.getAddress() != null) n.setAddress(src.getAddress());
        if (src.getAreaId() != null) n.setAreaId(src.getAreaId());
        if (src.getLongitude() != null) n.setLongitude(src.getLongitude());
        if (src.getLatitude() != null) n.setLatitude(src.getLatitude());
        if (src.getNodeType() != null) n.setNodeType(src.getNodeType());
        if (src.getFirmwareVersion() != null) n.setFirmwareVersion(src.getFirmwareVersion());
        if (src.getRssi() != null) n.setRssi(src.getRssi());
        if (src.getSnr() != null) n.setSnr(src.getSnr());
        if (src.getSpeed() != null) n.setSpeed(src.getSpeed());
        if (src.getBatteryVoltage() != null) n.setBatteryVoltage(src.getBatteryVoltage());
        if (src.getReportInterval() != null) n.setReportInterval(src.getReportInterval());
        if (src.getPic() != null) n.setPic(src.getPic());
        if (src.getUpdateBy() != null && !src.getUpdateBy().trim().isEmpty()) {
            n.setUpdateBy(src.getUpdateBy());
        }
        n.setUpdateTime(new Date());
        return nodeRepository.save(n);
    }

    /** 上报状态维护：在线状态 + 最后上报时间 + 信号指标。 */
    public void markReport(String nodeCode, Integer online, String rssi, String snr,
                           String speed, String batteryVoltage, String firmwareVersion) {
        Node n = nodeRepository.findByNodeCode(nodeCode).orElseGet(() -> {
            Node created = new Node();
            created.setNodeCode(nodeCode);
            created.setCreateTime(new Date());
            created.setIsDeleted(0);
            return created;
        });
        Date now = new Date();
        if (online != null) n.setOnline(online);
        n.setLastReportTime(now);
        if (rssi != null) n.setRssi(rssi);
        if (snr != null) n.setSnr(snr);
        if (speed != null) n.setSpeed(speed);
        if (batteryVoltage != null) n.setBatteryVoltage(batteryVoltage);
        if (firmwareVersion != null) n.setFirmwareVersion(firmwareVersion);
        n.setUpdateTime(now);
        nodeRepository.save(n);
    }

    public void delete(String id) {
        nodeRepository.deleteById(id);
    }
}
