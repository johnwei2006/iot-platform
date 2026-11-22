-- platform_java 业务表基线：设备 / 数据记录 / 告警
-- 传感器时序数据仍写 TDengine，设备实时状态仍用 Redis；后续结构变更请追加新 changeset。

CREATE TABLE IF NOT EXISTS `iot_device` (
  `id`              VARCHAR(36)  NOT NULL COMMENT '主键（迁移保留 Mongo id 或 UUID）',
  `device_id`       VARCHAR(128) NOT NULL COMMENT '设备编号',
  `node_id`         VARCHAR(128) DEFAULT NULL COMMENT '接入节点',
  `name`            VARCHAR(255) DEFAULT NULL,
  `identifier_type` VARCHAR(32)  DEFAULT NULL COMMENT 'VIN / TERMINALNO / DEVICEID',
  `auth_secret`     VARCHAR(128) DEFAULT NULL COMMENT '网关认证预共享密钥',
  `status`          VARCHAR(16)  DEFAULT NULL COMMENT 'ONLINE / OFFLINE',
  `org_code`        VARCHAR(32)  DEFAULT NULL COMMENT '所属机构编码（数据权限前缀匹配）',
  `owner_user_id`   VARCHAR(36)  DEFAULT NULL COMMENT '归属用户（SELF 数据权限）',
  `last_seen`       DATETIME(6)  DEFAULT NULL,
  `created_at`      DATETIME(6)  DEFAULT NULL,
  `updated_at`      DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_device_device_id` (`device_id`),
  KEY `idx_iot_device_org_code` (`org_code`),
  KEY `idx_iot_device_owner` (`owner_user_id`),
  KEY `idx_iot_device_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备';

CREATE TABLE IF NOT EXISTS `iot_data_record` (
  `id`                   VARCHAR(36)  NOT NULL,
  `device_id`            VARCHAR(128) NOT NULL,
  `service_type`         INT          NOT NULL DEFAULT 0,
  `service_version`      INT          NOT NULL DEFAULT 0,
  `service_data`         TEXT         DEFAULT NULL COMMENT '业务数据 base64',
  `event_id`             BIGINT       NOT NULL DEFAULT 0,
  `created_at`           DATETIME(6)  DEFAULT NULL,
  `service_data_decoded` JSON         DEFAULT NULL COMMENT 'dtm 解码结构化字段',
  `protocol_name`        VARCHAR(128) DEFAULT NULL,
  `transaction_type`     VARCHAR(128) DEFAULT NULL,
  `decode_status`        VARCHAR(32)  DEFAULT NULL,
  `decode_error`         VARCHAR(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_record_device_time` (`device_id`, `created_at`),
  KEY `idx_iot_record_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据记录（非时序原始读数）';

CREATE TABLE IF NOT EXISTS `iot_alarm` (
  `id`          VARCHAR(36)  NOT NULL,
  `device_id`   VARCHAR(128) DEFAULT NULL,
  `rule`        VARCHAR(128) DEFAULT NULL,
  `level`       VARCHAR(16)  DEFAULT NULL COMMENT 'INFO / WARN / ERROR',
  `message`     VARCHAR(1024) DEFAULT NULL,
  `status`      VARCHAR(16)  DEFAULT NULL COMMENT 'OPEN / RESOLVED',
  `created_at`  DATETIME(6)  DEFAULT NULL,
  `resolved_at` DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_alarm_device` (`device_id`),
  KEY `idx_iot_alarm_status` (`status`),
  KEY `idx_iot_alarm_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警';
