package com.iot.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启用定时调度（control 域：定时规则每分钟扫描 + 控制记录超时扫描）。 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
