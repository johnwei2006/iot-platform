-- 网关配置上报快照改名为网关配置信息表：iot_gateway_config_snapshot -> iot_gateway_config。
-- 字段按协议 0x09 配置字段命名；保留原有上报快照列以支持节点/传感器/设备等上报查询。
-- 新增 config_time（配置时间）、send_time（下发时间）、status（状态）；
-- status: SUCCESS-已生效 / PENDING-未下发 / FAILED-下发失败。
-- 每个网关最多允许一条 PENDING 或 FAILED 记录，由服务层保证。迁移幂等，可重复执行。

SET @oldTable = 'iot_gateway_config_snapshot';
SET @newTable = 'iot_gateway_config';
SET @oldCount = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@oldTable);
SET @newCount = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable);

SET @ddl = IF(@oldCount > 0 AND @newCount = 0,
  'RENAME TABLE `iot_gateway_config_snapshot` TO `iot_gateway_config`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'config_version';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `config_version` INT DEFAULT NULL COMMENT ''版本号''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'gateway_id';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `gateway_id` VARCHAR(64) DEFAULT NULL COMMENT ''网关编号''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'project_id';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `project_id` VARCHAR(64) DEFAULT NULL COMMENT ''项目编号''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'system_server_url';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `system_server_url` VARCHAR(255) DEFAULT NULL COMMENT ''系统服务器地址''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'system_server_port';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `system_server_port` INT DEFAULT NULL COMMENT ''服务器端口''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'heartbeat_interval';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `heartbeat_interval` INT DEFAULT NULL COMMENT ''心跳间隔''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'report_interval';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `report_interval` INT DEFAULT NULL COMMENT ''上报间隔''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'lora_freq_group';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `lora_freq_group` INT DEFAULT NULL COMMENT ''Lora频率组''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'config_time';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `config_time` DATETIME(6) DEFAULT NULL COMMENT ''配置时间''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'send_time';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `send_time` DATETIME(6) DEFAULT NULL COMMENT ''下发时间''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'status';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND COLUMN_NAME=@col)=0,
  'ALTER TABLE `iot_gateway_config` ADD COLUMN `status` VARCHAR(16) NOT NULL DEFAULT ''SUCCESS'' COMMENT ''状态：SUCCESS-已生效 / PENDING-未下发 / FAILED-下发失败''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=@newTable AND INDEX_NAME='idx_gw_config_status');
SET @ddl = IF(@idx=0,
  'ALTER TABLE `iot_gateway_config` ADD KEY `idx_gw_config_status` (`gateway_code`, `status`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史快照记录视为已生效，避免旧数据被误判为未下发记录。
UPDATE `iot_gateway_config` SET `status` = 'SUCCESS', `config_time` = COALESCE(`config_time`, `reported_at`)
WHERE `status` IS NULL OR `status` = '';

ALTER TABLE `iot_gateway_config` COMMENT = '网关配置信息';
