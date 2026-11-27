-- 内置类型收敛：网关 1-4G、2-公开、3-5G、4-其它；节点 1-采集、2-控制、3-虚拟、4-第三方。
-- 仅改 COMMENT；节点无“其它”类型，历史不匹配节点全部归入第三方(4)，旧采集(DATA=3)转为采集(1)。

ALTER TABLE `iot_gateway`
  MODIFY COLUMN `gateway_type` TINYINT(4) DEFAULT NULL COMMENT '网关类型：1-4G，2-公开，3-5G，4-其它';

ALTER TABLE `iot_node`
  MODIFY COLUMN `node_type` TINYINT(4) DEFAULT NULL COMMENT '节点类型：1-采集，2-控制，3-虚拟，4-第三方';

-- 网关：不匹配的旧值（含 NULL）归入其它
UPDATE `iot_gateway`
SET `gateway_type` = 4
WHERE `gateway_type` IS NULL OR `gateway_type` NOT IN (1,2,3,4);

-- 节点：先把所有非旧采集(DATA=3)的值（含 NULL / 旧 ACT / IRRI / 植保机 / 开度 / 6~8）归入第三方
UPDATE `iot_node`
SET `node_type` = 4
WHERE `node_type` IS NULL OR `node_type` <> 3;

-- 再把旧采集(DATA=3)转为新采集(1)
UPDATE `iot_node`
SET `node_type` = 1
WHERE `node_type` = 3;
