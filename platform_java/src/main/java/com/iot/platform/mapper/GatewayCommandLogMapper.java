package com.iot.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.GatewayCommandLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GatewayCommandLogMapper extends BaseMapper<GatewayCommandLog> { }
