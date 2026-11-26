-- iot_device -> iot_gateway 迁移：仅 device_id 列改名为 gateway_code，其它既有列保持不变；
-- 追加截图要求的 7 个字段：address/longitude/latitude/gateway_type/firmware_version/power/pic。
-- 旧表 iot_device 保留作备份，确认无误后由运维手动删除。

CREATE TABLE IF NOT EXISTS `iot_gateway` (
  `id`              VARCHAR(36)  NOT NULL COMMENT '主键（迁移保留 Mongo id 或 UUID）',
  `gateway_code`    VARCHAR(128) NOT NULL COMMENT '网关编码（原 device_id）',
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
  `address`         VARCHAR(200) DEFAULT NULL COMMENT '安装地点',
  `longitude`       VARCHAR(15)  DEFAULT NULL COMMENT '经度',
  `latitude`        VARCHAR(15)  DEFAULT NULL COMMENT '纬度',
  `gateway_type`    TINYINT(4)   DEFAULT NULL COMMENT '网关类型：1-4G网关，2-5G网关 ...',
  `firmware_version` VARCHAR(20) DEFAULT NULL COMMENT '固件版本号',
  `power`           VARCHAR(10)  DEFAULT NULL COMMENT '电池电压',
  `pic`             VARCHAR(100) DEFAULT NULL COMMENT '设备图片地址',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_gateway_gateway_code` (`gateway_code`),
  KEY `idx_iot_gateway_org_code` (`org_code`),
  KEY `idx_iot_gateway_owner` (`owner_user_id`),
  KEY `idx_iot_gateway_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关（由 iot_device 迁移）';

-- 幂等导入：仅当目标 id 不存在时插入，避免重复执行产生主键冲突。
INSERT INTO `iot_gateway` (
  `id`,`gateway_code`,`node_id`,`name`,`identifier_type`,`auth_secret`,`status`,
  `org_code`,`owner_user_id`,`last_seen`,`created_at`,`updated_at`
)
SELECT
  d.`id`,d.`device_id`,d.`node_id`,d.`name`,d.`identifier_type`,d.`auth_secret`,d.`status`,
  d.`org_code`,d.`owner_user_id`,d.`last_seen`,d.`created_at`,d.`updated_at`
FROM `iot_device` d
WHERE NOT EXISTS (SELECT 1 FROM `iot_gateway` g WHERE g.`id` = d.`id`);
