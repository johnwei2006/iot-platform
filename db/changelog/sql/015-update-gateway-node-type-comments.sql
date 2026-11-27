-- 内置网关/节点类型定义同步到列注释（仅改 COMMENT，不改数据类型与已有数据）。
-- 网关：1-4G网关，2-工控网关（原 5G 编码，历史数据保留）
-- 节点：3-采集节点，6-控制节点，7-虚拟节点，8-第三方节点；1/2/4/5 为旧类型，保留兼容

ALTER TABLE `iot_gateway`
  MODIFY COLUMN `gateway_type` TINYINT(4) DEFAULT NULL COMMENT '网关类型：1-4G网关，2-工控网关（原5G）';

ALTER TABLE `iot_node`
  MODIFY COLUMN `node_type` TINYINT(4) DEFAULT NULL COMMENT '节点类型：3-采集节点，6-控制节点，7-虚拟节点，8-第三方节点；1/2/4/5-旧类型';
