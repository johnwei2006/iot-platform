CREATE TABLE IF NOT EXISTS `iot_area` (
  `id` VARCHAR(36) NOT NULL COMMENT '主键',
  `org_code` VARCHAR(64) DEFAULT NULL COMMENT '归属机构',
  `area_name` VARCHAR(128) NOT NULL COMMENT '区域名称',
  `boundary` TEXT COMMENT '多边形边界（JSON 数组：[[lng,lat],...]）',
  `draw_area_mu` DECIMAL(18,4) DEFAULT NULL COMMENT '绘制面积（亩，由边界自动计算）',
  `label_area_mu` DECIMAL(18,4) DEFAULT NULL COMMENT '标注面积（亩，用户填写）',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME(6) DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME(6) DEFAULT NULL,
  `is_deleted` TINYINT DEFAULT 0,
  `owner_user_id` VARCHAR(36) DEFAULT NULL COMMENT '属主（SELF 数据权限）',
  PRIMARY KEY (`id`),
  KEY `idx_area_org` (`org_code`),
  KEY `idx_area_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域管理（卫星地图地块）';
