-- 网关上报历史信息：iot_gateway_heartbeat 重命名为 iot_gateway_info。
-- 列结构与 0x06 上报字段一致，仅改表名和索引名；迁移幂等，可重复执行。

SET @oldTable = 'iot_gateway_heartbeat';
SET @newTable = 'iot_gateway_info';
SET @oldCount = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@oldTable);
SET @newCount = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable);

SET @ddl = IF(@oldCount > 0 AND @newCount = 0,
  'RENAME TABLE `iot_gateway_heartbeat` TO `iot_gateway_info`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(@oldCount = 0 AND @newCount = 0,
  'CREATE TABLE `iot_gateway_info` (
    `id`                   VARCHAR(36)  NOT NULL COMMENT ''主键'',
    `gateway_code`         VARCHAR(64)  NOT NULL COMMENT ''网关编码'',
    `config_version`       INT          DEFAULT NULL COMMENT ''配置版本号'',
    `gateway_id`           VARCHAR(64)  DEFAULT NULL COMMENT ''上报网关编号'',
    `project_id`           VARCHAR(64)  DEFAULT NULL COMMENT ''项目编号'',
    `heartbeat_interval`   INT          DEFAULT NULL COMMENT ''心跳间隔'',
    `report_interval`      INT          DEFAULT NULL COMMENT ''传感器数据上报间隔'',
    `rtc_time`             VARCHAR(64)  DEFAULT NULL COMMENT ''网关RTC时间'',
    `system_server_url`    VARCHAR(255) DEFAULT NULL COMMENT ''系统服务器地址'',
    `system_server_port`   INT          DEFAULT NULL COMMENT ''业务服务器端口'',
    `hardware_version`     VARCHAR(64)  DEFAULT NULL COMMENT ''硬件版本'',
    `firmware_version`     VARCHAR(64)  DEFAULT NULL COMMENT ''固件版本'',
    `gateway_mcuid`        VARCHAR(64)  DEFAULT NULL COMMENT ''网关MCU ID'',
    `memory_total`         INT          DEFAULT NULL COMMENT ''总内存'',
    `memory_used`          INT          DEFAULT NULL COMMENT ''使用内存'',
    `flash_total`          INT          DEFAULT NULL COMMENT ''Flash总空间'',
    `flash_used`           INT          DEFAULT NULL COMMENT ''已用Flash'',
    `uplink_message_size`  INT          DEFAULT NULL COMMENT ''积压上行数据包'',
    `downlink_message_size` INT         DEFAULT NULL COMMENT ''积压下行数据包'',
    `battery`              VARCHAR(64)  DEFAULT NULL COMMENT ''电池电压'',
    `adapter_exists`       TINYINT      DEFAULT NULL COMMENT ''是否有适配器'',
    `charging_enabled`     TINYINT      DEFAULT NULL COMMENT ''是否充电'',
    `latest_reboot_time`   VARCHAR(64)  DEFAULT NULL COMMENT ''最近启动时间'',
    `payload`              JSON         DEFAULT NULL COMMENT ''解码后的完整上报内容'',
    `reported_at`          DATETIME(6)  NOT NULL COMMENT ''网关上报时间'',
    `created_at`           DATETIME(6)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_gw_info_gateway_time` (`gateway_code`, `reported_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''网关上报历史信息''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @oldIdx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND INDEX_NAME='idx_gw_hb_gateway_time');
SET @newIdx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND INDEX_NAME='idx_gw_info_gateway_time');
SET @ddl = IF(@oldIdx > 0 AND @newIdx = 0,
  'ALTER TABLE `iot_gateway_info` RENAME INDEX `idx_gw_hb_gateway_time` TO `idx_gw_info_gateway_time`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
