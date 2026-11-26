CREATE TABLE IF NOT EXISTS `iot_sensor_group` (
  `id` VARCHAR(36) NOT NULL COMMENT '主键',
  `org_code` VARCHAR(64) DEFAULT NULL COMMENT '归属机构',
  `group_name` VARCHAR(128) NOT NULL COMMENT '传感器组合名称',
  `sensor_codes` TEXT COMMENT '传感器编码列表（JSON 数组）',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME(6) DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME(6) DEFAULT NULL,
  `is_deleted` TINYINT DEFAULT 0,
  `owner_user_id` VARCHAR(36) DEFAULT NULL COMMENT '属主（SELF 数据权限）',
  PRIMARY KEY (`id`),
  KEY `idx_group_org` (`org_code`),
  KEY `idx_group_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器组合';
