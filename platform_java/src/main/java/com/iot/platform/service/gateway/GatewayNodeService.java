package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.model.Node;
import com.iot.platform.model.Sensor;
import com.iot.platform.repository.DeviceCtrlRepository;
import com.iot.platform.repository.NodeRepository;
import com.iot.platform.repository.SensorRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 网关节点视图：按网关查节点列表，附传感器/设备数量统计。 */
@Service
public class GatewayNodeService {

    private final NodeRepository nodeRepository;
    private final SensorRepository sensorRepository;
    private final DeviceCtrlRepository deviceCtrlRepository;

    public GatewayNodeService(NodeRepository nodeRepository,
                              SensorRepository sensorRepository,
                              DeviceCtrlRepository deviceCtrlRepository) {
        this.nodeRepository = nodeRepository;
        this.sensorRepository = sensorRepository;
        this.deviceCtrlRepository = deviceCtrlRepository;
    }

    /** 节点行：编码/名称/类型/入网时间(createTime)/上报频率 + 传感器与设备数量。 */
    public List<Map<String, Object>> listByGateway(String gatewayCode) {
        List<Node> nodes = nodeRepository.findAll(Wrappers.<Node>lambdaQuery()
                .eq(Node::getGatewayCode, gatewayCode)
                .eq(Node::getIsDeleted, 0)
                .orderByAsc(Node::getNodeCode));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Node node : nodes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeCode", node.getNodeCode());
            item.put("nodeName", node.getNodeName());
            item.put("nodeType", node.getNodeType());
            item.put("gatewayCode", node.getGatewayCode());
            item.put("joinTime", node.getCreateTime());
            item.put("reportInterval", node.getReportInterval());
            List<Sensor> sensors = sensorRepository.findByNodeCode(node.getNodeCode());
            List<DeviceCtrl> devices = deviceCtrlRepository.findByNodeCode(node.getNodeCode());
            item.put("sensorCount", sensors.size());
            item.put("deviceCount", devices.size());
            result.add(item);
        }
        return result;
    }
}
