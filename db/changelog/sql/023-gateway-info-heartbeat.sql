-- 0x06 网关信息上报落库：最新信息写入 iot_gateway，历史心跳写入 iot_gateway_heartbeat。
-- 列级幂等，避免迁移中断后重复执行失败；firmware_version 使用 iot_gateway 已有列。

SET @col = 'config_version';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `config_version` INT DEFAULT NULL COMMENT ''上报配置版本号'' AFTER `pic`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'gateway_id';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `gateway_id` VARCHAR(64) DEFAULT NULL COMMENT ''上报网关编号''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'project_id';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `project_id` VARCHAR(64) DEFAULT NULL COMMENT ''项目编号''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'heartbeat_interval';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `heartbeat_interval` INT DEFAULT NULL COMMENT ''心跳间隔''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'report_interval';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `report_interval` INT DEFAULT NULL COMMENT ''传感器数据上报间隔''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'rtc_time';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `rtc_time` VARCHAR(64) DEFAULT NULL COMMENT ''网关RTC时间''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'system_server_url';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `system_server_url` VARCHAR(255) DEFAULT NULL COMMENT ''系统服务器地址''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'system_server_port';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `system_server_port` INT DEFAULT NULL COMMENT ''业务服务器端口''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'hardware_version';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `hardware_version` VARCHAR(64) DEFAULT NULL COMMENT ''硬件版本''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'gateway_mcuid';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `gateway_mcuid` VARCHAR(64) DEFAULT NULL COMMENT ''网关MCU ID''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'memory_total';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `memory_total` INT DEFAULT NULL COMMENT ''总内存''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'memory_used';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `memory_used` INT DEFAULT NULL COMMENT ''使用内存''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'flash_total';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `flash_total` INT DEFAULT NULL COMMENT ''Flash总空间''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'flash_used';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `flash_used` INT DEFAULT NULL COMMENT ''已用Flash''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'uplink_message_size';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `uplink_message_size` INT DEFAULT NULL COMMENT ''积压上行数据包''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'downlink_message_size';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `downlink_message_size` INT DEFAULT NULL COMMENT ''积压下行数据包''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'battery';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `battery` VARCHAR(64) DEFAULT NULL COMMENT ''上报电池电压''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'adapter_exists';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `adapter_exists` TINYINT DEFAULT NULL COMMENT ''是否有适配器''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'charging_enabled';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `charging_enabled` TINYINT DEFAULT NULL COMMENT ''是否充电''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'latest_reboot_time';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `latest_reboot_time` VARCHAR(64) DEFAULT NULL COMMENT ''最近启动时间''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'info_reported_at';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='iot_gateway' AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway` ADD COLUMN `info_reported_at` DATETIME(6) DEFAULT NULL COMMENT ''网关信息上报时间''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `iot_gateway_heartbeat` (
  `id`                   VARCHAR(36)  NOT NULL COMMENT '主键',
  `gateway_code`         VARCHAR(64)  NOT NULL COMMENT '网关编码',
  `config_version`       INT          DEFAULT NULL COMMENT '配置版本号',
  `gateway_id`           VARCHAR(64)  DEFAULT NULL COMMENT '上报网关编号',
  `project_id`           VARCHAR(64)  DEFAULT NULL COMMENT '项目编号',
  `heartbeat_interval`   INT          DEFAULT NULL COMMENT '心跳间隔',
  `report_interval`      INT          DEFAULT NULL COMMENT '传感器数据上报间隔',
  `rtc_time`             VARCHAR(64)  DEFAULT NULL COMMENT '网关RTC时间',
  `system_server_url`    VARCHAR(255) DEFAULT NULL COMMENT '系统服务器地址',
  `system_server_port`   INT          DEFAULT NULL COMMENT '业务服务器端口',
  `hardware_version`     VARCHAR(64)  DEFAULT NULL COMMENT '硬件版本',
  `firmware_version`     VARCHAR(64)  DEFAULT NULL COMMENT '固件版本',
  `gateway_mcuid`        VARCHAR(64)  DEFAULT NULL COMMENT '网关MCU ID',
  `memory_total`         INT          DEFAULT NULL COMMENT '总内存',
  `memory_used`          INT          DEFAULT NULL COMMENT '使用内存',
  `flash_total`          INT          DEFAULT NULL COMMENT 'Flash总空间',
  `flash_used`           INT          DEFAULT NULL COMMENT '已用Flash',
  `uplink_message_size`  INT          DEFAULT NULL COMMENT '积压上行数据包',
  `downlink_message_size` INT         DEFAULT NULL COMMENT '积压下行数据包',
  `battery`              VARCHAR(64)  DEFAULT NULL COMMENT '电池电压',
  `adapter_exists`       TINYINT      DEFAULT NULL COMMENT '是否有适配器',
  `charging_enabled`     TINYINT      DEFAULT NULL COMMENT '是否充电',
  `latest_reboot_time`   VARCHAR(64)  DEFAULT NULL COMMENT '最近启动时间',
  `payload`              JSON         DEFAULT NULL COMMENT '解码后的完整上报内容',
  `reported_at`          DATETIME(6)  NOT NULL COMMENT '网关上报时间',
  `created_at`           DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gw_hb_gateway_time` (`gateway_code`, `reported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关心跳上报记录';
