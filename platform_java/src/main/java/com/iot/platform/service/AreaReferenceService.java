package com.iot.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.DeviceCtrlMapper;
import com.iot.platform.mapper.GatewayMapper;
import com.iot.platform.mapper.NodeMapper;
import com.iot.platform.mapper.SensorMapper;
import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.model.Gateway;
import com.iot.platform.model.Node;
import com.iot.platform.model.Sensor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 区域引用统计：统计区域在四类实体中的引用数，供删除保护使用。 */
@Service
public class AreaReferenceService {

    @Autowired
    private GatewayMapper gatewayMapper;

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private SensorMapper sensorMapper;

    @Autowired
    private DeviceCtrlMapper deviceCtrlMapper;

    /** 返回四类实体的引用数量；Gateway 表无逻辑删除列，其余按 isDeleted=0 统计。 */
    public Map<String, Long> countByArea(String areaId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (areaId == null || areaId.trim().isEmpty()) {
            counts.put("gateway", 0L);
            counts.put("node", 0L);
            counts.put("sensor", 0L);
            counts.put("deviceCtrl", 0L);
            counts.put("total", 0L);
            return counts;
        }
        String id = areaId.trim();
        long gateway = gatewayMapper.selectCount(Wrappers.<Gateway>lambdaQuery().eq(Gateway::getAreaId, id));
        long node = nodeMapper.selectCount(Wrappers.<Node>lambdaQuery()
                .eq(Node::getAreaId, id).eq(Node::getIsDeleted, 0));
        long sensor = sensorMapper.selectCount(Wrappers.<Sensor>lambdaQuery()
                .eq(Sensor::getAreaId, id).eq(Sensor::getIsDeleted, 0));
        long deviceCtrl = deviceCtrlMapper.selectCount(Wrappers.<DeviceCtrl>lambdaQuery()
                .eq(DeviceCtrl::getAreaId, id).eq(DeviceCtrl::getIsDeleted, 0));
        counts.put("gateway", gateway);
        counts.put("node", node);
        counts.put("sensor", sensor);
        counts.put("deviceCtrl", deviceCtrl);
        counts.put("total", gateway + node + sensor + deviceCtrl);
        return counts;
    }

    /** 构造引用提示文案，例如“网关 2 / 节点 1”。 */
    public String describe(Map<String, Long> counts) {
        if (counts == null || counts.getOrDefault("total", 0L) == 0L) {
            return "";
        }
        return "网关 " + counts.getOrDefault("gateway", 0L)
                + " / 节点 " + counts.getOrDefault("node", 0L)
                + " / 传感器 " + counts.getOrDefault("sensor", 0L)
                + " / 控制设备 " + counts.getOrDefault("deviceCtrl", 0L);
    }
}
