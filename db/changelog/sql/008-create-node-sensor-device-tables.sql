-- 物联网三层架构：网关(iot_gateway) -> 节点(iot_node) -> 传感器(iot_sensor)/控制设备(iot_device_ctrl)
-- 含 4 张分类字典表（sensor_class/sensor_class_type/device_class/device_class_type）与常用种子数据。
-- 参考用户建库脚本，表名统一加 iot_ 前缀；主键沿用 String UUID（IdType.NONE）。

CREATE TABLE IF NOT EXISTS `iot_node` (
  `id`              VARCHAR(36)  NOT NULL COMMENT '主键',
  `org_code`        VARCHAR(16)  DEFAULT NULL COMMENT '机构编码',
  `node_code`       VARCHAR(50)  NOT NULL COMMENT '节点编码',
  `node_name`       VARCHAR(50)  DEFAULT NULL COMMENT '节点名称',
  `gateway_code`    VARCHAR(50)  DEFAULT NULL COMMENT '网关编码（挂靠 iot_gateway.gateway_code）',
  `status`          TINYINT(4)   DEFAULT '0' COMMENT '状态（无用）',
  `address`         VARCHAR(200) DEFAULT NULL COMMENT '安装地点',
  `longitude`       VARCHAR(15)  DEFAULT NULL COMMENT '经度',
  `latitude`        VARCHAR(15)  DEFAULT NULL COMMENT '纬度',
  `node_type`       TINYINT(4)   DEFAULT NULL COMMENT '节点类型，1-ACT，2-IRRI，3-DATA，4-植保机，5-开度',
  `firmware_version` VARCHAR(20) DEFAULT NULL COMMENT '固件版本号',
  `rssi`            VARCHAR(10)  DEFAULT NULL COMMENT '信号质量',
  `snr`             VARCHAR(10)  DEFAULT NULL COMMENT '信噪比',
  `speed`           VARCHAR(10)  DEFAULT NULL COMMENT '速率',
  `battery_voltage` VARCHAR(10)  DEFAULT NULL COMMENT '电池电压',
  `report_interval` INT(11)      DEFAULT NULL COMMENT '上报频率',
  `last_report_time` DATETIME    DEFAULT NULL COMMENT '最后上报时间',
  `online`          TINYINT(4)   DEFAULT '1' COMMENT '是否在线',
  `pic`             VARCHAR(100) DEFAULT NULL COMMENT '设备图片地址',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '更新人',
  `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`      TINYINT(4)   DEFAULT '0' COMMENT '逻辑删除状态, 0-正常, 1-删除',
  `owner_user_id`   VARCHAR(36)  DEFAULT NULL COMMENT '归属用户（SELF 数据权限）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `iot_node_node_code_IDX` (`node_code`),
  KEY `iot_node_gateway_code_IDX` (`gateway_code`),
  KEY `iot_node_org_code_IDX` (`org_code`),
  KEY `iot_node_owner_IDX` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点（挂在网关下）';

CREATE TABLE IF NOT EXISTS `iot_sensor_class` (
  `class`         VARCHAR(5)   NOT NULL COMMENT '大类编码',
  `class_name`    VARCHAR(255) DEFAULT NULL COMMENT '大类名称',
  `class_name_en` VARCHAR(255) DEFAULT NULL COMMENT '大类英文名',
  `unit`          VARCHAR(30)  DEFAULT NULL COMMENT '单位',
  `scale`         INT(3)       DEFAULT NULL COMMENT '小数位',
  PRIMARY KEY (`class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器大类字典';

CREATE TABLE IF NOT EXISTS `iot_sensor_class_type` (
  `class_type`      VARCHAR(50)  NOT NULL COMMENT '传感器类型编码',
  `class`           VARCHAR(50)  DEFAULT NULL COMMENT '传感器大类编码',
  `class_type_name` VARCHAR(255) DEFAULT NULL COMMENT '传感器类型名称',
  `model`           VARCHAR(255) DEFAULT NULL COMMENT '规格型号',
  `description`     VARCHAR(255) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`class_type`),
  KEY `iot_sensor_class_type_class_IDX` (`class`)
  -- 不建物理外键：项目其余表均无外键，保持一致；级联约束由应用层维护
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器小类字典';

CREATE TABLE IF NOT EXISTS `iot_sensor` (
  `id`               VARCHAR(36)  NOT NULL COMMENT '主键',
  `org_code`         VARCHAR(16)  DEFAULT NULL COMMENT '机构编码',
  `sensor_code`      VARCHAR(50)  NOT NULL COMMENT '传感器编码',
  `sensor_name`      VARCHAR(50)  DEFAULT NULL COMMENT '传感器名称',
  `node_code`        VARCHAR(50)  DEFAULT NULL COMMENT '节点编码',
  `sensor_class`     VARCHAR(6)   DEFAULT NULL COMMENT '传感器大类编码',
  `sensor_class_type` VARCHAR(8)  DEFAULT NULL COMMENT '传感器类型编码',
  `status`           TINYINT(4)   DEFAULT '0' COMMENT '状态，0-正常，1-异常',
  `unit`             VARCHAR(15)  DEFAULT NULL COMMENT '单位',
  `warn_min`         DOUBLE       DEFAULT NULL COMMENT '最小预警值（传感器级阈值，优先于网关级 MetricThreshold）',
  `warn_max`         DOUBLE       DEFAULT NULL COMMENT '最大预警值',
  `scale`            INT(11)      DEFAULT '2' COMMENT '小数位',
  `report_interval`  INT(11)      DEFAULT NULL COMMENT '上报频率',
  `last_report_time` DATETIME     DEFAULT NULL COMMENT '最后上报时间',
  `create_by`        VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`      DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_by`        VARCHAR(50)  DEFAULT NULL COMMENT '更新人',
  `update_time`      DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`       TINYINT(4)   DEFAULT '0' COMMENT '逻辑删除状态, 0-正常, 1-删除',
  `pic`              VARCHAR(200) DEFAULT NULL COMMENT '图片地址',
  `channel`          VARCHAR(255) DEFAULT NULL COMMENT '端口号---Data节点是实际端口号；4G网关上是虚拟端口号',
  `command_read`     VARCHAR(64)  DEFAULT NULL COMMENT '读数指令（485 Hex格式，直接发送给modbus）---可空，接入虚拟节点下485传感器时非空',
  `owner_user_id`    VARCHAR(36)  DEFAULT NULL COMMENT '归属用户（SELF 数据权限）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `iot_sensor_sensor_code_IDX` (`sensor_code`),
  KEY `iot_sensor_node_code_IDX` (`node_code`),
  KEY `iot_sensor_org_code_IDX` (`org_code`),
  KEY `iot_sensor_owner_IDX` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器（挂在节点下，按通道区分）';

CREATE TABLE IF NOT EXISTS `iot_device_class` (
  `class`      VARCHAR(5)  NOT NULL COMMENT '大类编码',
  `class_name` VARCHAR(32) DEFAULT NULL COMMENT '大类名称',
  `switch_mode` INT(5)     DEFAULT NULL COMMENT '开关模式（1-开度；2-2开设备；3-三开设备）',
  `on_name`    VARCHAR(32) DEFAULT NULL COMMENT '打开名称',
  `off_name`   VARCHAR(32) DEFAULT NULL COMMENT '关闭名称',
  `stop_name`  VARCHAR(32) DEFAULT NULL COMMENT '停止名称',
  `set_name`   VARCHAR(50) DEFAULT NULL COMMENT '设置名称',
  PRIMARY KEY (`class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制设备大类字典';

CREATE TABLE IF NOT EXISTS `iot_device_class_type` (
  `type`          VARCHAR(32)  NOT NULL COMMENT '设备类型编码（四位: device_class+XX）',
  `type_name`     VARCHAR(32)  DEFAULT NULL COMMENT '设备类型名称',
  `device_class`  VARCHAR(5)   NOT NULL COMMENT '设备大类编码',
  `model`         VARCHAR(255) DEFAULT NULL COMMENT '厂商/品牌名称/规格型号',
  `desc_info`     VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`type`),
  KEY `iot_device_class_type_class_IDX` (`device_class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制设备小类字典';

CREATE TABLE IF NOT EXISTS `iot_device_ctrl` (
  `id`                VARCHAR(36)  NOT NULL COMMENT '主键',
  `org_code`          VARCHAR(16)  DEFAULT NULL COMMENT '机构编码',
  `device_code`       VARCHAR(50)  NOT NULL COMMENT '控制设备编码',
  `device_name`       VARCHAR(50)  DEFAULT NULL COMMENT '控制设备名称',
  `device_class`      VARCHAR(5)   DEFAULT NULL COMMENT '控制设备大类编码',
  `device_class_type` VARCHAR(32)  DEFAULT NULL COMMENT '控制设备类型编码',
  `node_code`         VARCHAR(50)  DEFAULT NULL COMMENT '节点编码',
  `status`            VARCHAR(50)  DEFAULT NULL COMMENT '状态(0关，1开，2正转，3反转，4暂停。0~100开度值,任意浮点数输出值)',
  `online`            TINYINT(4)   DEFAULT '1' COMMENT '是否在线（0-离线；1-在线）',
  `auto`              TINYINT(4)   DEFAULT '1' COMMENT '是否自动（0-手动；1-自动）',
  `address`           VARCHAR(200) DEFAULT NULL COMMENT '安装地址',
  `longitude`         VARCHAR(15)  DEFAULT NULL COMMENT '经度',
  `latitude`          VARCHAR(15)  DEFAULT NULL COMMENT '纬度',
  `pic`               VARCHAR(100) DEFAULT NULL COMMENT '设备图片地址',
  `create_by`         VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `create_time`       DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_by`         VARCHAR(50)  DEFAULT NULL COMMENT '更新人',
  `update_time`       DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`        TINYINT(4)   DEFAULT '0' COMMENT '逻辑删除状态, 0-正常, 1-删除',
  `channel`           VARCHAR(255) DEFAULT NULL COMMENT '端口号',
  `connect_type`      INT(2)       DEFAULT NULL COMMENT '连接形式',
  `owner_user_id`     VARCHAR(36)  DEFAULT NULL COMMENT '归属用户（SELF 数据权限）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `iot_device_ctrl_device_code_IDX` (`device_code`),
  KEY `iot_device_ctrl_node_code_IDX` (`node_code`),
  KEY `iot_device_ctrl_org_code_IDX` (`org_code`),
  KEY `iot_device_ctrl_owner_IDX` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制设备（挂在节点下，按通道区分）';

-- ===== 字典种子数据（INSERT IGNORE：主键冲突即跳过，幂等） =====

INSERT IGNORE INTO `iot_sensor_class` (`class`,`class_name`,`class_name_en`,`unit`,`scale`) VALUES
  ('TH','空气温湿度','AirTempHumidity','℃/%RH',1),
  ('SO','土壤','Soil','%',2),
  ('WS','气象','WeatherStation','',1),
  ('LQ','水质','WaterQuality','mg/L',2),
  ('EC','电导率','Conductivity','us/cm',1);

INSERT IGNORE INTO `iot_sensor_class_type` (`class_type`,`class`,`class_type_name`,`model`,`description`) VALUES
  ('TH01','TH','空气温度传感器','','测量空气温度'),
  ('TH02','TH','空气湿度传感器','','测量空气相对湿度'),
  ('SO01','SO','土壤温度传感器','','测量土壤温度'),
  ('SO02','SO','土壤湿度传感器','','测量土壤体积含水率'),
  ('WS01','WS','风速传感器','',''),
  ('WS02','WS','风向传感器','',''),
  ('LQ01','LQ','溶解氧传感器','','');

INSERT IGNORE INTO `iot_device_class` (`class`,`class_name`,`switch_mode`,`on_name`,`off_name`,`stop_name`,`set_name`) VALUES
  ('F01','风机',2,'打开','关闭','',''),
  ('P01','水泵',2,'启动','停止','',''),
  ('W01','开度窗',1,'全开','全关','停止','设置开度'),
  ('I01','灌溉阀',2,'开阀','关阀','',''),
  ('M01','三开设备',3,'正转','反转','暂停','');

INSERT IGNORE INTO `iot_device_class_type` (`type`,`type_name`,`device_class`,`model`,`desc_info`,`updated_time`) VALUES
  ('F0101','普通风机','F01','','380V 三相风机',NOW()),
  ('P0101','普通水泵','P01','','单相离心泵',NOW()),
  ('W0101','推杆开度窗','W01','','0~100% 开度',NOW()),
  ('I0101','电磁阀','I01','','DN50 电磁阀',NOW()),
  ('M0101','三开卷帘机','M01','','正反转+停止',NOW());

-- ===== 为现有用户的 data_scopes 补充新实体 key（避免默认 DENY_ALL） =====
-- sys_user.data_scopes 为 JSON map（实体编码→范围），缺失 key 时数据权限默认拒绝。
-- 幂等：仅当 data_scopes 中不含该 key 时写入 {"node":"ALL","sensor":"ALL","device_ctrl":"ALL"}。
UPDATE `sys_user`
SET `data_scopes` = JSON_MERGE_PATCH(`data_scopes`, '{"node":"ALL","sensor":"ALL","device_ctrl":"ALL"}')
WHERE `data_scopes` IS NOT NULL
  AND (JSON_EXTRACT(`data_scopes`, '$.node') IS NULL
       OR JSON_EXTRACT(`data_scopes`, '$.sensor') IS NULL
       OR JSON_EXTRACT(`data_scopes`, '$.device_ctrl') IS NULL);
