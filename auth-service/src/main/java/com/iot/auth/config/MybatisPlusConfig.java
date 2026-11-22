package com.iot.auth.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.iot.auth.mapper")
public class MybatisPlusConfig {
}
