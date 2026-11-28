package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.model.GatewayInfoHistory;
import com.iot.platform.repository.GatewayInfoHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 网关上报历史信息分页查询服务。 */
@Service
public class GatewayInfoHistoryService {

    private final GatewayInfoHistoryRepository repository;

    public GatewayInfoHistoryService(GatewayInfoHistoryRepository repository) { this.repository = repository; }

    public Map<String, Object> page(String gatewayCode, int pageNo, int pageSize) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, Math.min(pageSize, 200));
        var wrapper = Wrappers.<GatewayInfoHistory>lambdaQuery();
        if (gatewayCode != null && !gatewayCode.isEmpty()) {
            wrapper.eq(GatewayInfoHistory::getGatewayCode, gatewayCode);
        }
        wrapper.orderByDesc(GatewayInfoHistory::getReportedAt);
        var result = repository.findPage(new Page<>(safePage, safeSize), wrapper);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());
        return response;
    }
}
