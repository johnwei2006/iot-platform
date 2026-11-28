package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.model.GatewayInfoHistory;

import java.util.List;

public interface GatewayInfoHistoryRepository {

    List<GatewayInfoHistory> findAll(Wrapper<GatewayInfoHistory> wrapper);

    IPage<GatewayInfoHistory> findPage(Page<GatewayInfoHistory> page, Wrapper<GatewayInfoHistory> wrapper);

    GatewayInfoHistory save(GatewayInfoHistory record);
}
