package com.iot.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.model.Gateway;
import com.iot.platform.model.Node;
import com.iot.platform.model.Sensor;
import com.iot.platform.repository.DeviceCtrlRepository;
import com.iot.platform.repository.GatewayRepository;
import com.iot.platform.repository.NodeRepository;
import com.iot.platform.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 生产管理服务：生产阶段设备入库与分配。
 *
 * <p>与业务管理的差异：设备在创建时不属于任何机构（orgCode 为空），
 * 编号由后端按「前缀 + 类型片段 + 日期 + 当日流水」规则生成，分配网关到机构时
 * 所属节点、传感器与控制设备同步级联分配。</p>
 */
@Service
public class ProductionService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private GatewayRepository gatewayRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private SensorRepository sensorRepository;
    @Autowired
    private DeviceCtrlRepository deviceCtrlRepository;

    /* ---------- 编号生成 ---------- */

    /**
     * 按规则生成设备编号：前缀 + 类型片段 + yyyyMMdd + 4 位当日流水。
     * 示例：4G 网关 GW4G202611270001、采集节点 NDCD202611270001。
     *
     * @param kind 设备大类（决定前缀与编号所在表）
     * @param sub  类型片段（如 4G/CD/温湿度大类缩写），可为空
     */
    public synchronized String generateCode(Kind kind, String sub) {
        String day = LocalDate.now().format(DAY);
        String prefix = kind.prefix + (sub == null ? "" : sub) + day;
        List<String> codes = kind.codes(this, prefix);
        int max = 0;
        for (String code : codes) {
            String tail = code.substring(prefix.length());
            try {
                max = Math.max(max, Integer.parseInt(tail));
            } catch (NumberFormatException ignored) {
                // 非本规则生成的编码，跳过
            }
        }
        return prefix + String.format("%04d", max + 1);
    }

    /** 设备大类：前缀 + 编号所在表。 */
    public enum Kind {
        GATEWAY("GW") {
            @Override List<String> codes(ProductionService svc, String prefix) {
                return svc.gatewayRepository.findAll(Wrappers.<Gateway>lambdaQuery()
                        .likeRight(Gateway::getDeviceId, prefix)).stream().map(Gateway::getDeviceId).toList();
            }
        },
        NODE("ND") {
            @Override List<String> codes(ProductionService svc, String prefix) {
                return svc.nodeRepository.findAll(Wrappers.<Node>lambdaQuery()
                        .likeRight(Node::getNodeCode, prefix)).stream().map(Node::getNodeCode).toList();
            }
        },
        SENSOR("SE") {
            @Override List<String> codes(ProductionService svc, String prefix) {
                return svc.sensorRepository.findAll(Wrappers.<Sensor>lambdaQuery()
                        .likeRight(Sensor::getSensorCode, prefix)).stream().map(Sensor::getSensorCode).toList();
            }
        },
        CTRL("DE") {
            @Override List<String> codes(ProductionService svc, String prefix) {
                return svc.deviceCtrlRepository.findAll(Wrappers.<DeviceCtrl>lambdaQuery()
                        .likeRight(DeviceCtrl::getDeviceCode, prefix)).stream().map(DeviceCtrl::getDeviceCode).toList();
            }
        };

        final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }

        abstract List<String> codes(ProductionService service, String prefix);
    }

    /** 网关类型片段：1-4G，2-5G。 */
    static String gatewaySub(Integer gatewayType) {
        if (gatewayType == null) return "GN";
        return switch (gatewayType) {
            case 1 -> "4G";
            case 2 -> "5G";
            default -> "GN";
        };
    }

    /** 节点类型片段：1-ACT，2-IRRI，3-DATA(采集)，4-植保机，5-开度。 */
    static String nodeSub(Integer nodeType) {
        if (nodeType == null) return "GN";
        return switch (nodeType) {
            case 1 -> "AC";
            case 2 -> "IR";
            case 3 -> "CD";
            case 4 -> "ZB";
            case 5 -> "KD";
            default -> "GN";
        };
    }

    /** 大类编码片段：取大类编码中的字母数字转大写，最多 2 位；为空回退 GN。 */
    static String classSub(String clazz) {
        if (clazz == null) return "GN";
        String letters = clazz.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return letters.isEmpty() ? "GN" : letters.substring(0, Math.min(2, letters.length()));
    }

    private String randomSecret() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(Character.toUpperCase(Character.forDigit(secureRandom.nextInt(16), 16)));
        }
        return sb.toString();
    }

    /* ---------- 生产入库（orgCode 保持为空） ---------- */

    /** 创建生产网关：编号自动生成，预共享密钥随机生成，不属于任何机构。 */
    public Gateway createGateway(Gateway src, String subject) {
        Date now = new Date();
        src.setDeviceId(generateCode(Kind.GATEWAY, gatewaySub(src.getGatewayType())));
        src.setId(java.util.UUID.randomUUID().toString());
        src.setCreatedAt(now);
        src.setUpdatedAt(now);
        src.setStatus("OFFLINE");
        if (src.getAuthSecret() == null || src.getAuthSecret().isBlank()) {
            src.setAuthSecret(randomSecret());
        }
        src.setOrgCode(null);
        src.setOwnerUserId(subject);
        return gatewayRepository.save(src);
    }

    /** 创建生产节点：编号自动生成，不属于任何机构。 */
    public Node createNode(Node src, String subject) {
        Date now = new Date();
        src.setNodeCode(generateCode(Kind.NODE, nodeSub(src.getNodeType())));
        src.setId(java.util.UUID.randomUUID().toString());
        src.setCreateTime(now);
        src.setUpdateTime(now);
        src.setCreateBy(subject);
        src.setUpdateBy(subject);
        src.setIsDeleted(0);
        src.setOnline(0);
        src.setOrgCode(null);
        src.setOwnerUserId(subject);
        return nodeRepository.save(src);
    }

    /** 创建生产传感器：编号自动生成，不属于任何机构。 */
    public Sensor createSensor(Sensor src, String subject) {
        Date now = new Date();
        src.setSensorCode(generateCode(Kind.SENSOR, classSub(src.getSensorClass())));
        src.setId(java.util.UUID.randomUUID().toString());
        src.setCreateTime(now);
        src.setUpdateTime(now);
        src.setCreateBy(subject);
        src.setUpdateBy(subject);
        src.setIsDeleted(0);
        src.setStatus(0);
        src.setOrgCode(null);
        src.setOwnerUserId(subject);
        return sensorRepository.save(src);
    }

    /** 创建生产控制设备：编号自动生成，不属于任何机构。 */
    public DeviceCtrl createCtrl(DeviceCtrl src, String subject) {
        Date now = new Date();
        src.setDeviceCode(generateCode(Kind.CTRL, classSub(src.getDeviceClass())));
        src.setId(java.util.UUID.randomUUID().toString());
        src.setCreateTime(now);
        src.setUpdateTime(now);
        src.setCreateBy(subject);
        src.setUpdateBy(subject);
        src.setIsDeleted(0);
        src.setOnline(0);
        src.setAuto(0);
        src.setStatus("0");
        src.setOrgCode(null);
        src.setOwnerUserId(subject);
        return deviceCtrlRepository.save(src);
    }

    /* ---------- 设备列表 ---------- */

    private static <T> com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> unassigned(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> wrapper,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> orgCode) {
        return wrapper.and(w -> w.isNull(orgCode).or().eq(orgCode, ""));
    }

    /** 全部网关（含已分配），供生产列表展示分配状态。 */
    public List<Gateway> listAllGateways() {
        return gatewayRepository.findAll();
    }

    /** 全部节点（含已分配）。 */
    public List<Node> listAllNodes() {
        return nodeRepository.findAll(Wrappers.<Node>lambdaQuery().eq(Node::getIsDeleted, 0));
    }

    /** 全部传感器（含已分配）。 */
    public List<Sensor> listAllSensors() {
        return sensorRepository.findAll(Wrappers.<Sensor>lambdaQuery().eq(Sensor::getIsDeleted, 0));
    }

    /** 全部控制设备（含已分配）。 */
    public List<DeviceCtrl> listAllCtrls() {
        return deviceCtrlRepository.findAll(Wrappers.<DeviceCtrl>lambdaQuery().eq(DeviceCtrl::getIsDeleted, 0));
    }

    /** 未分配机构的网关列表（归属下拉与分配操作候选）。 */
    public List<Gateway> listUnassignedGateways() {
        return gatewayRepository.findAll(unassigned(Wrappers.<Gateway>lambdaQuery(), Gateway::getOrgCode));
    }

    /** 未分配机构的节点列表（归属下拉候选）。 */
    public List<Node> listUnassignedNodes() {
        return nodeRepository.findAll(unassigned(Wrappers.<Node>lambdaQuery(), Node::getOrgCode)
                .eq(Node::getIsDeleted, 0));
    }

    /** 未分配机构的传感器列表。 */
    public List<Sensor> listUnassignedSensors() {
        return sensorRepository.findAll(unassigned(Wrappers.<Sensor>lambdaQuery(), Sensor::getOrgCode)
                .eq(Sensor::getIsDeleted, 0));
    }

    /** 未分配机构的控制设备列表。 */
    public List<DeviceCtrl> listUnassignedCtrls() {
        return deviceCtrlRepository.findAll(unassigned(Wrappers.<DeviceCtrl>lambdaQuery(), DeviceCtrl::getOrgCode)
                .eq(DeviceCtrl::getIsDeleted, 0));
    }

    /* ---------- 更新与删除（仅未分配设备允许，已分配设备锁定） ---------- */

    private static boolean isUnassigned(String orgCode) {
        return orgCode == null || orgCode.isBlank();
    }

    /** 更新生产网关（名称/类型/固件），已分配到机构则拒绝。 */
    public Gateway updateGatewayIfUnassigned(String id, Gateway patch) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("gateway not found: " + id));
        if (!isUnassigned(gateway.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        if (patch.getName() != null) gateway.setName(patch.getName());
        if (patch.getGatewayType() != null) gateway.setGatewayType(patch.getGatewayType());
        if (patch.getFirmwareVersion() != null) gateway.setFirmwareVersion(patch.getFirmwareVersion());
        if (patch.getPic() != null) gateway.setPic(patch.getPic());
        if (patch.getLongitude() != null) gateway.setLongitude(patch.getLongitude());
        if (patch.getLatitude() != null) gateway.setLatitude(patch.getLatitude());
        gateway.setUpdatedAt(new Date());
        return gatewayRepository.save(gateway);
    }

    /** 更新生产节点（名称/类型/归属网关/固件/上报周期），已分配到机构则拒绝。 */
    public Node updateNodeIfUnassigned(String id, Node patch) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("node not found: " + id));
        if (!isUnassigned(node.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        if (patch.getNodeName() != null) node.setNodeName(patch.getNodeName());
        if (patch.getNodeType() != null) node.setNodeType(patch.getNodeType());
        if (patch.getGatewayCode() != null) node.setGatewayCode(patch.getGatewayCode().isBlank() ? null : patch.getGatewayCode());
        if (patch.getFirmwareVersion() != null) node.setFirmwareVersion(patch.getFirmwareVersion());
        if (patch.getReportInterval() != null) node.setReportInterval(patch.getReportInterval());
        if (patch.getPic() != null) node.setPic(patch.getPic());
        node.setUpdateTime(new Date());
        return nodeRepository.save(node);
    }

    /** 更新生产传感器（属性/归属节点/通道），已分配到机构则拒绝。 */
    public Sensor updateSensorIfUnassigned(String id, Sensor patch) {
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("sensor not found: " + id));
        if (!isUnassigned(sensor.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        if (patch.getSensorName() != null) sensor.setSensorName(patch.getSensorName());
        if (patch.getSensorClass() != null) sensor.setSensorClass(patch.getSensorClass());
        if (patch.getSensorClassType() != null) sensor.setSensorClassType(patch.getSensorClassType());
        if (patch.getUnit() != null) sensor.setUnit(patch.getUnit());
        if (patch.getChannel() != null) sensor.setChannel(patch.getChannel());
        if (patch.getNodeCode() != null) sensor.setNodeCode(patch.getNodeCode().isBlank() ? null : patch.getNodeCode());
        if (patch.getWarnMin() != null) sensor.setWarnMin(patch.getWarnMin());
        if (patch.getWarnMax() != null) sensor.setWarnMax(patch.getWarnMax());
        if (patch.getScale() != null) sensor.setScale(patch.getScale());
        if (patch.getReportInterval() != null) sensor.setReportInterval(patch.getReportInterval());
        if (patch.getCommandRead() != null) sensor.setCommandRead(patch.getCommandRead());
        if (patch.getPic() != null) sensor.setPic(patch.getPic());
        sensor.setUpdateTime(new Date());
        return sensorRepository.save(sensor);
    }

    /** 更新生产控制设备（属性/归属节点/通道/连接方式），已分配到机构则拒绝。 */
    public DeviceCtrl updateCtrlIfUnassigned(String id, DeviceCtrl patch) {
        DeviceCtrl device = deviceCtrlRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("device not found: " + id));
        if (!isUnassigned(device.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        if (patch.getDeviceName() != null) device.setDeviceName(patch.getDeviceName());
        if (patch.getDeviceClass() != null) device.setDeviceClass(patch.getDeviceClass());
        if (patch.getDeviceClassType() != null) device.setDeviceClassType(patch.getDeviceClassType());
        if (patch.getChannel() != null) device.setChannel(patch.getChannel());
        if (patch.getNodeCode() != null) device.setNodeCode(patch.getNodeCode().isBlank() ? null : patch.getNodeCode());
        if (patch.getConnectType() != null) device.setConnectType(patch.getConnectType());
        if (patch.getPic() != null) device.setPic(patch.getPic());
        device.setUpdateTime(new Date());
        return deviceCtrlRepository.save(device);
    }

    /** 删除生产网关（物理删除），已分配到机构则拒绝。 */
    public void deleteGatewayIfUnassigned(String id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("gateway not found: " + id));
        if (!isUnassigned(gateway.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        gatewayRepository.deleteById(id);
    }

    /** 删除生产节点（逻辑删除），已分配到机构则拒绝。 */
    public void deleteNodeIfUnassigned(String id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("node not found: " + id));
        if (!isUnassigned(node.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        nodeRepository.deleteById(id);
    }

    /** 删除生产传感器（逻辑删除），已分配到机构则拒绝。 */
    public void deleteSensorIfUnassigned(String id) {
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("sensor not found: " + id));
        if (!isUnassigned(sensor.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        sensorRepository.deleteById(id);
    }

    /** 删除生产控制设备（逻辑删除），已分配到机构则拒绝。 */
    public void deleteCtrlIfUnassigned(String id) {
        DeviceCtrl device = deviceCtrlRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("device not found: " + id));
        if (!isUnassigned(device.getOrgCode())) {
            throw new DeviceAssignedException();
        }
        deviceCtrlRepository.deleteById(id);
    }

    /** 已分配到机构的设备拒绝在生产模块中修改/删除。 */
    public static class DeviceAssignedException extends RuntimeException {
        public DeviceAssignedException() {
            super("device already assigned to an organization");
        }
    }

    /* ---------- 级联分配机构 ---------- */

    /**
     * 分配网关到机构：网关及其下节点、传感器、控制设备同步写入 orgCode。
     *
     * @return 级联分配数量统计
     */
    @Transactional
    public AssignResult assignGateway(String gatewayId, String orgCode, String subject) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new RuntimeException("gateway not found: " + gatewayId));
        if (orgCode == null || orgCode.isBlank()) {
            throw new RuntimeException("orgCode is required");
        }
        Date now = new Date();
        gateway.setOrgCode(orgCode);
        gateway.setUpdatedAt(now);
        gatewayRepository.save(gateway);

        AssignResult result = new AssignResult();
        List<Node> nodes = nodeRepository.findAll(Wrappers.<Node>lambdaQuery()
                .eq(Node::getGatewayCode, gateway.getDeviceId())
                .eq(Node::getIsDeleted, 0));
        for (Node node : nodes) {
            node.setOrgCode(orgCode);
            node.setUpdateBy(subject);
            node.setUpdateTime(now);
            nodeRepository.save(node);
            result.nodes++;

            List<Sensor> sensors = sensorRepository.findAll(Wrappers.<Sensor>lambdaQuery()
                    .eq(Sensor::getNodeCode, node.getNodeCode())
                    .eq(Sensor::getIsDeleted, 0));
            for (Sensor sensor : sensors) {
                sensor.setOrgCode(orgCode);
                sensor.setUpdateBy(subject);
                sensor.setUpdateTime(now);
                sensorRepository.save(sensor);
                result.sensors++;
            }

            List<DeviceCtrl> ctrls = deviceCtrlRepository.findAll(Wrappers.<DeviceCtrl>lambdaQuery()
                    .eq(DeviceCtrl::getNodeCode, node.getNodeCode())
                    .eq(DeviceCtrl::getIsDeleted, 0));
            for (DeviceCtrl ctrl : ctrls) {
                ctrl.setOrgCode(orgCode);
                ctrl.setUpdateBy(subject);
                ctrl.setUpdateTime(now);
                deviceCtrlRepository.save(ctrl);
                result.devices++;
            }
        }
        return result;
    }

    /** 级联分配数量统计。 */
    public static class AssignResult {
        public int nodes;
        public int sensors;
        public int devices;
    }

    /** 按 id 查找网关（更新场景）。 */
    public Optional<Gateway> findGateway(String id) {
        return gatewayRepository.findById(id);
    }
}
