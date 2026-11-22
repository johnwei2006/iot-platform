-- 产品 / 产品-菜单 / 机构-产品授权体系（追加 changeset，不改旧表）
-- 产品是功能上限；机构管理员在角色中进一步勾选具体菜单。

CREATE TABLE IF NOT EXISTS `sys_product` (
  `id`          VARCHAR(36)  NOT NULL COMMENT '主键',
  `code`        VARCHAR(64)  NOT NULL COMMENT '产品编码（SYS/SCREEN/IOT/FARM，可扩展）',
  `name`        VARCHAR(128) NOT NULL COMMENT '产品名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '产品说明',
  `sort`        VARCHAR(16)  DEFAULT '0' COMMENT '排序',
  `status`      VARCHAR(16)  DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
  `built_in`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '预置产品不可删除',
  `created_at`  DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_product_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品主数据';

CREATE TABLE IF NOT EXISTS `sys_product_menu` (
  `id`         VARCHAR(36) NOT NULL,
  `product_id` VARCHAR(36) NOT NULL COMMENT 'sys_product.id',
  `menu_id`    VARCHAR(36) NOT NULL COMMENT 'sys_menu.id',
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_product_menu` (`product_id`, `menu_id`),
  KEY `idx_sys_product_menu_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品包含的菜单范围';

CREATE TABLE IF NOT EXISTS `sys_product_org` (
  `id`         VARCHAR(36) NOT NULL,
  `product_id` VARCHAR(36) NOT NULL COMMENT 'sys_product.id',
  `org_id`     VARCHAR(36) NOT NULL COMMENT 'sys_org.id',
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_product_org` (`product_id`, `org_id`),
  KEY `idx_sys_product_org_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构已分配产品';

-- 预置产品：幂等插入，重复执行保持不变
INSERT IGNORE INTO `sys_product` (`id`, `code`, `name`, `description`, `sort`, `status`, `built_in`, `created_at`) VALUES
  ('00000000-0000-0000-0000-000000000001', 'SYS', '系统管理', '平台与机构管理功能', '5', 'ENABLED', 1, NOW()),
  ('00000000-0000-0000-0000-000000000002', 'SCREEN', '大屏', '数据大屏展示', '10', 'ENABLED', 1, NOW()),
  ('00000000-0000-0000-0000-000000000003', 'IOT', '物联网', '看数据、设备控制与消息任务', '20', 'ENABLED', 1, NOW()),
  ('00000000-0000-0000-0000-000000000004', 'FARM', '农事管理', '农事记录与地块档案', '30', 'ENABLED', 1, NOW());
