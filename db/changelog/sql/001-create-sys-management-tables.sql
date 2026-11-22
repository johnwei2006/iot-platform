-- 系统管理表基线：机构 / 角色 / 菜单按钮 / 用户
-- 幂等建表：已有表与数据保持不变；后续结构变更请追加新 changeset，不要修改本文件。

CREATE TABLE IF NOT EXISTS `sys_org` (
  `id`          VARCHAR(36)  NOT NULL COMMENT '主键（历史 Mongo ObjectId 十六进制字符串或 UUID）',
  `code`        VARCHAR(32)  NOT NULL COMMENT '机构编码（4/6/8/10 位数字）',
  `parent_code` VARCHAR(32)  DEFAULT NULL COMMENT '上级机构编码，一级机构为 NULL',
  `name`        VARCHAR(128) NOT NULL COMMENT '机构名称',
  `leader`      VARCHAR(64)  DEFAULT NULL COMMENT '负责人',
  `phone`       VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
  `sort`        VARCHAR(16)  DEFAULT '0' COMMENT '排序',
  `status`      VARCHAR(16)  DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
  `created_at`  DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_org_code` (`code`),
  KEY `idx_sys_org_parent_code` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统机构';

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`          VARCHAR(36)  NOT NULL,
  `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码（SUPER_ADMIN 等）',
  `name`        VARCHAR(128) NOT NULL,
  `menu_ids`    JSON         DEFAULT NULL COMMENT '授权菜单/按钮 id 列表',
  `data_scopes` JSON         DEFAULT NULL COMMENT '业务实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF',
  `built_in`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '内置角色不可删除',
  `status`      VARCHAR(16)  DEFAULT 'ENABLED',
  `created_at`  DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id`         VARCHAR(36)  NOT NULL,
  `parent_id`  VARCHAR(36)  DEFAULT NULL COMMENT '父节点 id，根节点为 NULL',
  `name`       VARCHAR(128) NOT NULL,
  `type`       VARCHAR(16)  NOT NULL COMMENT 'DIR / MENU / BUTTON',
  `path`       VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
  `component`  VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
  `perm`       VARCHAR(128) DEFAULT NULL COMMENT '按钮权限标识，非空时唯一',
  `icon`       VARCHAR(128) DEFAULT NULL,
  `sort`       VARCHAR(16)  DEFAULT '0',
  `status`     VARCHAR(16)  DEFAULT 'ENABLED',
  `created_at` DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_menu_perm` (`perm`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单与按钮';

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`            VARCHAR(36)  NOT NULL,
  `username`      VARCHAR(128) DEFAULT NULL,
  `password_hash` VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt 哈希，禁止明文',
  `openid`        VARCHAR(128) DEFAULT NULL,
  `unionid`       VARCHAR(128) DEFAULT NULL,
  `phone`         VARCHAR(64)  DEFAULT NULL,
  `roles`         JSON         DEFAULT NULL COMMENT '角色编码列表',
  `org_id`        VARCHAR(36)  DEFAULT NULL COMMENT '所属机构 id（sys_org.id）',
  `org_code`      VARCHAR(32)  DEFAULT NULL COMMENT '所属机构编码（层级前缀冗余）',
  `data_scopes`   JSON         DEFAULT NULL COMMENT '业务实体编码 -> 数据权限范围',
  `token_version` BIGINT       NOT NULL DEFAULT 0 COMMENT '递增使历史 token 全部失效',
  `status`        VARCHAR(16)  DEFAULT 'ENABLED',
  `created_at`    DATETIME(6)  DEFAULT NULL,
  `last_login_at` DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_openid` (`openid`),
  UNIQUE KEY `uk_sys_user_unionid` (`unionid`),
  UNIQUE KEY `uk_sys_user_phone` (`phone`),
  KEY `idx_sys_user_org_code` (`org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';
