-- 条件控制规则支持生效日期范围（定时规则仍使用 start_time/end_time 存日期）。
-- 新增独立字段，避免与条件规则原有的 HH:mm 时段（start_time/end_time）语义冲突。
-- 幂等：列已存在时跳过。

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'iot_control_rule'
    AND COLUMN_NAME = 'effective_from'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `iot_control_rule` ADD COLUMN `effective_from` VARCHAR(10) DEFAULT NULL COMMENT ''条件规则生效日期 YYYY-MM-DD'' AFTER `end_time`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'iot_control_rule'
    AND COLUMN_NAME = 'effective_to'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `iot_control_rule` ADD COLUMN `effective_to` VARCHAR(10) DEFAULT NULL COMMENT ''条件规则失效日期 YYYY-MM-DD'' AFTER `effective_from`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
