-- 网关管理：配置上报快照、下发命令日志、网关本地任务
-- 幂等建表 + 索引，不影响已有业务数据。

CREATE TABLE IF NOT EXISTS `iot_gateway_config_snapshot` (
  `id`           VARCHAR(36)  NOT NULL COMMENT '主键',
  `gateway_code` VARCHAR(64)  NOT NULL COMMENT '网关编码',
  `service_type` INT          NOT NULL COMMENT '协议服务类型（6/8/12/14/16/20/38）',
  `category`     VARCHAR(32)  NOT NULL COMMENT 'INFO/CONFIG/NODE/SENSOR/DEVICE/TASK',
  `payload`      JSON         NOT NULL COMMENT '解码后的上报内容',
  `reported_at`  DATETIME(6)  DEFAULT NULL COMMENT '网关上报时间',
  `created_at`   DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gw_cfg_gateway_type_time` (`gateway_code`, `service_type`, `reported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关配置上报快照';

CREATE TABLE IF NOT EXISTS `iot_gateway_command_log` (
  `id`           VARCHAR(36)  NOT NULL COMMENT '主键',
  `gateway_code` VARCHAR(64)  NOT NULL COMMENT '网关编码',
  `service_type` INT          NOT NULL COMMENT '下发服务类型（9/13/17/21/37/101）',
  `command_type` VARCHAR(32)  NOT NULL COMMENT 'RADIO/NODE/SENSOR/DEVICE/TASK/REBOOT',
  `request_id`   VARCHAR(64)  DEFAULT NULL COMMENT '幂等键',
  `payload`      JSON         NOT NULL COMMENT '下发的解码内容',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/TIMEOUT',
  `result`       VARCHAR(16)  DEFAULT NULL COMMENT '网关反馈结果',
  `reason`       VARCHAR(255) DEFAULT NULL COMMENT '失败原因摘要',
  `sent_at`      DATETIME(6)  DEFAULT NULL,
  `finished_at`  DATETIME(6)  DEFAULT NULL,
  `created_by`   VARCHAR(36)  DEFAULT NULL,
  `created_at`   DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gw_cmd_request` (`request_id`),
  KEY `idx_gw_cmd_gateway_type_time` (`gateway_code`, `service_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关配置下发命令日志';

CREATE TABLE IF NOT EXISTS `iot_gateway_task` (
  `id`           VARCHAR(36)  NOT NULL COMMENT '主键',
  `gateway_code` VARCHAR(64)  NOT NULL COMMENT '网关编码',
  `task_name`    VARCHAR(128) NOT NULL COMMENT '任务名称',
  `task_no`      INT          NOT NULL COMMENT '网关任务编号',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `trigger_type` VARCHAR(16)  NOT NULL DEFAULT 'TIME' COMMENT '触发类型：TIME/CONDITION',
  `trigger_time` VARCHAR(16)  DEFAULT NULL COMMENT '触发时间 HH:mm:ss',
  `triggers`     JSON         DEFAULT NULL COMMENT '触发条件数组',
  `actions`      JSON         NOT NULL COMMENT '动作数组',
  `deploy_status` VARCHAR(16) DEFAULT NULL COMMENT '下发状态：PENDING/SUCCESS/FAILED',
  `feedback_msg` VARCHAR(255) DEFAULT NULL COMMENT '0x26 反馈摘要',
  `org_code`     VARCHAR(32)  DEFAULT NULL,
  `owner_user_id` VARCHAR(36) DEFAULT NULL,
  `created_at`   DATETIME(6)  DEFAULT NULL,
  `updated_at`   DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gw_task_gateway` (`gateway_code`, `task_no`),
  KEY `idx_gw_task_org` (`org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关本地任务定义';
