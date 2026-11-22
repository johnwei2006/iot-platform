package com.iot.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** 注意：TDengine 的 JdbcTemplate 由 TdEngineConfig 手工创建，与本配置中的 MySQL Mapper 互不干扰。 */
@Configuration
@MapperScan("com.iot.platform.mapper")
public class MybatisPlusConfig {
}
