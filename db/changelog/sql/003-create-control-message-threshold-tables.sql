-- 控制规则 / 控制记录 / 测点阈值 / 任务 / 消息 / 通知偏好
-- 设备控制与消息任务域新增业务表；时序数据仍写 TDengine，实时状态仍用 Redis。

CREATE TABLE IF NOT EXISTS `iot_control_rule` (
  `id`             VARCHAR(36)  NOT NULL COMMENT '主键',
  `name`           VARCHAR(128) NOT NULL COMMENT '规则名称',
  `kind`           VARCHAR(16)  NOT NULL COMMENT '组(GROUP) / 定时(TIMED) / 条件(CONDITION)',
  `trigger`        VARCHAR(16)  NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式：MANUAL / TIMED / CONDITION',
  `devices`        JSON         NOT NULL COMMENT '受控设备列表 [{"deviceId","deviceName","action"}]',
  `action`         VARCHAR(16)  DEFAULT NULL COMMENT '动作：ON / OFF / OPEN / CLOSE / STOP / LEVEL',
  `time`           VARCHAR(8)   DEFAULT NULL COMMENT '定时执行时间 HH:mm',
  `close_time`     VARCHAR(8)   DEFAULT NULL COMMENT '可选反向动作时间 HH:mm',
  `metric`         VARCHAR(64)  DEFAULT NULL COMMENT '条件：取数指标',
  `station`        VARCHAR(32)  DEFAULT NULL COMMENT '条件：测点位置（棚口/棚中）',
  `operator`       VARCHAR(8)   DEFAULT NULL COMMENT '比较符：GT / GTE / LT / LTE / EQ',
  `threshold`      DECIMAL(10,2) DEFAULT NULL COMMENT '条件：阈值',
  `start_time`     VARCHAR(8)   DEFAULT NULL COMMENT '条件生效时段开始 HH:mm',
  `end_time`       VARCHAR(8)   DEFAULT NULL COMMENT '条件生效时段结束 HH:mm',
  `use_return`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用恢复反向动作',
  `return_threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '恢复阈值',
  `plot_id`        VARCHAR(36)  DEFAULT NULL COMMENT '关联大棚',
  `org_code`       VARCHAR(32)  DEFAULT NULL COMMENT '所属机构编码（数据权限前缀匹配）',
  `owner_user_id`  VARCHAR(36)  DEFAULT NULL COMMENT '创建人（SELF 数据权限）',
  `enabled`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用（启用前须通过冲突校验）',
  `created_by`     VARCHAR(36)  DEFAULT NULL,
  `created_at`     DATETIME(6)  DEFAULT NULL,
  `updated_at`     DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_rule_org` (`org_code`),
  KEY `idx_iot_rule_owner` (`owner_user_id`),
  KEY `idx_iot_rule_enabled` (`enabled`),
  KEY `idx_iot_rule_kind` (`kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制规则（组合/定时/条件）';

CREATE TABLE IF NOT EXISTS `iot_control_log` (
  `id`           VARCHAR(36)  NOT NULL,
  `rule_id`      VARCHAR(36)  DEFAULT NULL COMMENT '触发规则（手动下发为空）',
  `rule_name`    VARCHAR(128) DEFAULT NULL,
  `device_id`    VARCHAR(128) NOT NULL,
  `device_name`  VARCHAR(255) DEFAULT NULL,
  `source`       VARCHAR(16)  NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL / RULE / GROUP',
  `plot_id`      VARCHAR(36)  DEFAULT NULL,
  `action`       VARCHAR(16)  NOT NULL COMMENT '动作：ON / OFF / OPEN / CLOSE / STOP / LEVEL',
  `action_value` VARCHAR(16)  DEFAULT NULL COMMENT '开度等附加参数',
  `result`       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUCCESS / TIMEOUT / FAILED / PARTIAL',
  `detail`       VARCHAR(1024) DEFAULT NULL COMMENT '设备反馈详情',
  `org_code`     VARCHAR(32)  DEFAULT NULL,
  `created_at`   DATETIME(6)  DEFAULT NULL,
  `finished_at`  DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_clog_device` (`device_id`, `created_at`),
  KEY `idx_iot_clog_rule` (`rule_id`),
  KEY `idx_iot_clog_result` (`result`),
  KEY `idx_iot_clog_created` (`created_at`),
  KEY `idx_iot_clog_org` (`org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制执行记录';

CREATE TABLE IF NOT EXISTS `iot_metric_threshold` (
  `id`         VARCHAR(36)  NOT NULL,
  `device_id`  VARCHAR(128) NOT NULL COMMENT '传感器设备编号',
  `metric`     VARCHAR(64)  NOT NULL COMMENT '指标（温度/湿度/CO2 等）',
  `station`    VARCHAR(32)  DEFAULT NULL COMMENT '测点位置（棚口/棚中），空表设备级',
  `min_value`  DECIMAL(10,2) DEFAULT NULL COMMENT '下限',
  `max_value`  DECIMAL(10,2) DEFAULT NULL COMMENT '上限',
  `org_code`   VARCHAR(32)  DEFAULT NULL,
  `updated_by` VARCHAR(36)  DEFAULT NULL,
  `created_at` DATETIME(6)  DEFAULT NULL,
  `updated_at` DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_threshold` (`device_id`, `metric`, `station`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测点阈值（超限联动告警）';

CREATE TABLE IF NOT EXISTS `iot_task` (
  `id`               VARCHAR(36)  NOT NULL,
  `title`            VARCHAR(255) NOT NULL,
  `content`          VARCHAR(2048) DEFAULT NULL,
  `assignee_user_id` VARCHAR(36)  NOT NULL COMMENT '执行人',
  `due_time`         DATETIME(6)  DEFAULT NULL COMMENT '截止时间（超时态标记）',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'TODO' COMMENT 'TODO / DONE',
  `done_time`        DATETIME(6)  DEFAULT NULL,
  `done_note`        VARCHAR(1024) DEFAULT NULL COMMENT '完成说明',
  `org_code`         VARCHAR(32)  DEFAULT NULL,
  `created_by`       VARCHAR(36)  DEFAULT NULL,
  `created_at`       DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_task_assignee` (`assignee_user_id`, `status`),
  KEY `idx_iot_task_org` (`org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务';

CREATE TABLE IF NOT EXISTS `iot_message` (
  `id`         VARCHAR(36)  NOT NULL,
  `user_id`    VARCHAR(36)  NOT NULL COMMENT '接收用户',
  `type`       VARCHAR(16)  NOT NULL COMMENT 'TASK / ALARM / SYSTEM',
  `ref_id`     VARCHAR(36)  DEFAULT NULL COMMENT '关联对象（任务/告警/控制记录 id）',
  `title`      VARCHAR(255) NOT NULL,
  `content`    VARCHAR(2048) DEFAULT NULL,
  `read_at`    DATETIME(6)  DEFAULT NULL COMMENT '已读时间（未读角标）',
  `created_at` DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iot_msg_user` (`user_id`, `created_at`),
  KEY `idx_iot_msg_read` (`user_id`, `read_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息';

CREATE TABLE IF NOT EXISTS `iot_notify_pref` (
  `id`            VARCHAR(36) NOT NULL,
  `user_id`       VARCHAR(36) NOT NULL COMMENT '用户（唯一）',
  `receive_kinds` JSON        DEFAULT NULL COMMENT '接收类型开关 ["TASK","ALARM","SYSTEM"]',
  `channels`      JSON        DEFAULT NULL COMMENT '渠道开关（本期仅存配置不实际推送）',
  `updated_at`    DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_notify_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知偏好';
