package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.config.TdEngineConfig;
import com.iot.platform.config.TdEngineProperties;
import com.iot.platform.model.Sensor;
import com.iot.platform.model.SensorReading;
import com.iot.platform.repository.SensorRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** 传感器服务：节点下挂靠的第三层采集实体。 */
@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);
    private static final int HISTORY_LIMIT = 2000;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired(required = false)
    @Qualifier("tdEngineJdbcTemplate")
    private JdbcTemplate tdEngineJdbcTemplate;

    @Autowired(required = false)
    private TdEngineProperties tdEngineProperties;

    public List<Sensor> list() {
        return sensorRepository.findAll();
    }

    public List<Sensor> listByCriteria(DataScopeQuery scope) {
        if (scope == null || scope.getType() == DataScopeQuery.Type.ALL) {
            return sensorRepository.findAll();
        }
        if (scope.getType() == DataScopeQuery.Type.DENY_ALL) {
            return List.of();
        }
        LambdaQueryWrapper<Sensor> wrapper = Wrappers.<Sensor>lambdaQuery().eq(Sensor::getIsDeleted, 0);
        switch (scope.getType()) {
            case ORG_AND_CHILDREN -> wrapper.likeRight(Sensor::getOrgCode, scope.getValue());
            case ORG_ONLY -> wrapper.eq(Sensor::getOrgCode, scope.getValue());
            case SELF -> wrapper.eq(Sensor::getOwnerUserId, scope.getValue());
            default -> {
                return List.of();
            }
        }
        return sensorRepository.findAll(wrapper);
    }

    public List<Sensor> listByNodeCode(String nodeCode) {
        return sensorRepository.findByNodeCode(nodeCode);
    }

    public Optional<Sensor> get(String id) {
        return sensorRepository.findById(id);
    }

    public Optional<Sensor> getBySensorCode(String sensorCode) {
        return sensorRepository.findBySensorCode(sensorCode);
    }

    public Sensor create(Sensor s, String subject, String creatorOrgCode) {
        Date now = new Date();
        s.setCreateTime(now);
        s.setUpdateTime(now);
        s.setCreateBy(subject);
        s.setUpdateBy(subject);
        if (s.getIsDeleted() == null) {
            s.setIsDeleted(0);
        }
        if (s.getStatus() == null) {
            s.setStatus(0);
        }
        if (s.getScale() == null) {
            s.setScale(2);
        }
        if (s.getOrgCode() == null || s.getOrgCode().trim().isEmpty()) {
            s.setOrgCode(creatorOrgCode);
        }
        s.setOwnerUserId(subject);
        return sensorRepository.save(s);
    }

    public Sensor update(String id, Sensor src) {
        Sensor s = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("sensor not found: " + id));
        if (src.getSensorName() != null) s.setSensorName(src.getSensorName());
        if (src.getNodeCode() != null) s.setNodeCode(src.getNodeCode());
        if (src.getSensorClass() != null) s.setSensorClass(src.getSensorClass());
        if (src.getSensorClassType() != null) s.setSensorClassType(src.getSensorClassType());
        if (src.getStatus() != null) s.setStatus(src.getStatus());
        if (src.getUnit() != null) s.setUnit(src.getUnit());
        if (src.getWarnMin() != null) s.setWarnMin(src.getWarnMin());
        if (src.getWarnMax() != null) s.setWarnMax(src.getWarnMax());
        if (src.getScale() != null) s.setScale(src.getScale());
        if (src.getReportInterval() != null) s.setReportInterval(src.getReportInterval());
        if (src.getPic() != null) s.setPic(src.getPic());
        if (src.getChannel() != null) s.setChannel(src.getChannel());
        if (src.getCommandRead() != null) s.setCommandRead(src.getCommandRead());
        if (src.getUpdateBy() != null && !src.getUpdateBy().trim().isEmpty()) {
            s.setUpdateBy(src.getUpdateBy());
        }
        s.setUpdateTime(new Date());
        return sensorRepository.save(s);
    }

    /**
     * 按传感器编码查询时序曲线。TDengine 不可用时返回空列表，避免影响业务页面。
     */
    public List<SensorReading> history(String sensorCode, long from, long to) {
        if (tdEngineJdbcTemplate == null || tdEngineProperties == null
                || sensorCode == null || sensorCode.trim().isEmpty() || from > to) {
            return List.of();
        }
        String table = TdEngineConfig.sanitizeIdentifier(tdEngineProperties.getDatabase())
                + "." + TdEngineConfig.sanitizeIdentifier(tdEngineProperties.getStable());
        String sql = "SELECT ts, sensorvalue FROM " + table
                + " WHERE sensorcode = ? AND ts >= ? AND ts <= ? ORDER BY ts ASC LIMIT " + HISTORY_LIMIT;
        try {
            return tdEngineJdbcTemplate.query(sql,
                    (rs, rowNum) -> new SensorReading(rs.getLong(1), null, null, null,
                            sensorCode, null, rs.getDouble(2)),
                    sensorCode, new Timestamp(from), new Timestamp(to));
        } catch (RuntimeException e) {
            log.warn("TDengine history query failed: sensorCode={} message={}", sensorCode, e.getMessage());
            return List.of();
        }
    }

    /** 上报状态维护：最后上报时间 + 状态。 */
    public void markReport(String sensorCode, Integer status) {
        Sensor s = sensorRepository.findBySensorCode(sensorCode).orElseGet(() -> {
            Sensor created = new Sensor();
            created.setSensorCode(sensorCode);
            created.setCreateTime(new Date());
            created.setIsDeleted(0);
            return created;
        });
        s.setLastReportTime(new Date());
        if (status != null) s.setStatus(status);
        s.setUpdateTime(new Date());
        sensorRepository.save(s);
    }

    public void delete(String id) {
        sensorRepository.deleteById(id);
    }
}
