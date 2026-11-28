-- 统一设备编码规则数据迁移（一次性，Liquibase 跟踪执行）：
--   网关   11 位：类型 2 位(1-4G→10, 2-工控→30, 3-5G→20, 其它→40) + YYMMDD(created_at) + 3 位组内流水
--   节点   11 位：类型 2 位(1-采集→a0, 2-控制→b0, 3-虚拟→c0, 其它→d0) + YYMMDD(create_time) + 3 位组内流水
--   传感器 13 位：归属节点新编码首两位第二位改 1 + 2 位通道号
--   控制设备 13 位：归属节点新编码首两位第二位改 2 + 2 位通道号
-- 同步改写所有编码引用：节点/传感器/设备归属列、网关上报/配置/命令日志/任务、
-- 控制规则 devices JSON(nodeId/deviceId)、控制日志、传感器组合 sensor_codes、
-- 阈值/告警/数据记录的 device_id（历史值同域替换）。
-- 说明：业务表排序规则不一致（早期表 utf8mb4_unicode_ci、021/023 后建表 utf8mb4_0900_ai_ci），
-- 临时表列与全部关联比较统一显式 COLLATE utf8mb4_unicode_ci 规避 mix 错误。

-- ===== 1) 生成新旧编码映射（临时表，仅本变更集会话可见） =====

CREATE TEMPORARY TABLE tmp_gw_map AS
SELECT t.id, t.old_code, CONCAT(t.pref, t.dt, LPAD(t.rn, 3, '0')) COLLATE utf8mb4_unicode_ci AS new_code FROM (
  SELECT id, gateway_code COLLATE utf8mb4_unicode_ci AS old_code,
    CASE gateway_type WHEN 1 THEN '10' WHEN 3 THEN '20' WHEN 2 THEN '30' ELSE '40' END AS pref,
    DATE_FORMAT(COALESCE(created_at, NOW()), '%y%m%d') AS dt,
    ROW_NUMBER() OVER (
      PARTITION BY CASE gateway_type WHEN 1 THEN '10' WHEN 3 THEN '20' WHEN 2 THEN '30' ELSE '40' END,
                   DATE_FORMAT(COALESCE(created_at, NOW()), '%y%m%d')
      ORDER BY COALESCE(created_at, NOW()), gateway_code) AS rn
  FROM iot_gateway
) t;

CREATE TEMPORARY TABLE tmp_node_map AS
SELECT t.id, t.old_code, CONCAT(t.pref, t.dt, LPAD(t.rn, 3, '0')) COLLATE utf8mb4_unicode_ci AS new_code FROM (
  SELECT id, node_code COLLATE utf8mb4_unicode_ci AS old_code,
    CASE node_type WHEN 1 THEN 'a0' WHEN 2 THEN 'b0' WHEN 3 THEN 'c0' ELSE 'd0' END AS pref,
    DATE_FORMAT(COALESCE(create_time, NOW()), '%y%m%d') AS dt,
    ROW_NUMBER() OVER (
      PARTITION BY CASE node_type WHEN 1 THEN 'a0' WHEN 2 THEN 'b0' WHEN 3 THEN 'c0' ELSE 'd0' END,
                   DATE_FORMAT(COALESCE(create_time, NOW()), '%y%m%d')
      ORDER BY COALESCE(create_time, NOW()), node_code) AS rn
  FROM iot_node WHERE is_deleted = 0
) t;

CREATE TEMPORARY TABLE tmp_sensor_map AS
SELECT s.id, s.sensor_code COLLATE utf8mb4_unicode_ci AS old_code,
  CONCAT(LEFT(n.new_code, 1), '1', SUBSTRING(n.new_code, 2),
         LPAD(COALESCE(NULLIF(TRIM(s.channel), ''), '0'), 2, '0')) COLLATE utf8mb4_unicode_ci AS new_code
FROM iot_sensor s
JOIN tmp_node_map n ON s.node_code = n.old_code
WHERE s.is_deleted = 0;

CREATE TEMPORARY TABLE tmp_ctrl_map AS
SELECT d.id, d.device_code COLLATE utf8mb4_unicode_ci AS old_code,
  CONCAT(LEFT(n.new_code, 1), '2', SUBSTRING(n.new_code, 2),
         LPAD(COALESCE(NULLIF(TRIM(d.channel), ''), '0'), 2, '0')) COLLATE utf8mb4_unicode_ci AS new_code
FROM iot_device_ctrl d
JOIN tmp_node_map n ON d.node_code = n.old_code
WHERE d.is_deleted = 0;

-- ===== 2) 改写引用（按旧编码关联映射；比较侧显式 COLLATE 规避排序规则混用） =====

-- 网关编码引用
UPDATE iot_node n JOIN tmp_gw_map m ON n.gateway_code = m.old_code COLLATE utf8mb4_unicode_ci SET n.gateway_code = m.new_code;
UPDATE iot_gateway_info t JOIN tmp_gw_map m ON t.gateway_code = m.old_code COLLATE utf8mb4_unicode_ci SET t.gateway_code = m.new_code;
UPDATE iot_gateway_config t JOIN tmp_gw_map m ON t.gateway_code = m.old_code COLLATE utf8mb4_unicode_ci SET t.gateway_code = m.new_code;
UPDATE iot_gateway_command_log t JOIN tmp_gw_map m ON t.gateway_code = m.old_code COLLATE utf8mb4_unicode_ci SET t.gateway_code = m.new_code;
UPDATE iot_gateway_task t JOIN tmp_gw_map m ON t.gateway_code = m.old_code COLLATE utf8mb4_unicode_ci SET t.gateway_code = m.new_code;
UPDATE iot_metric_threshold t JOIN tmp_gw_map m ON t.device_id = m.old_code COLLATE utf8mb4_unicode_ci SET t.device_id = m.new_code;
UPDATE iot_alarm t JOIN tmp_gw_map m ON t.device_id = m.old_code COLLATE utf8mb4_unicode_ci SET t.device_id = m.new_code;
UPDATE iot_data_record t JOIN tmp_gw_map m ON t.device_id = m.old_code COLLATE utf8mb4_unicode_ci SET t.device_id = m.new_code;

-- 节点编码引用
UPDATE iot_sensor s JOIN tmp_node_map m ON s.node_code = m.old_code COLLATE utf8mb4_unicode_ci SET s.node_code = m.new_code;
UPDATE iot_device_ctrl d JOIN tmp_node_map m ON d.node_code = m.old_code COLLATE utf8mb4_unicode_ci SET d.node_code = m.new_code;
UPDATE iot_control_log t JOIN tmp_node_map m ON t.node_id = m.old_code COLLATE utf8mb4_unicode_ci SET t.node_id = m.new_code;

-- 控制规则 devices JSON：nodeId / deviceId 同步替换（保留其余键值）
UPDATE iot_control_rule r
JOIN (
  SELECT r2.id AS rid,
    JSON_ARRAYAGG(JSON_OBJECT(
      'deviceId', COALESCE(cm.new_code, jt.deviceId),
      'deviceName', jt.deviceName,
      'nodeId', COALESCE(nm.new_code, jt.nodeId),
      'channel', jt.channel,
      'deviceType', jt.deviceType)) AS devs
  FROM iot_control_rule r2,
  JSON_TABLE(r2.devices, '$[*]' COLUMNS (
      deviceId VARCHAR(64) PATH '$.deviceId',
      deviceName VARCHAR(128) PATH '$.deviceName',
      nodeId VARCHAR(64) PATH '$.nodeId',
      channel INT PATH '$.channel',
      deviceType VARCHAR(32) PATH '$.deviceType')) jt
  LEFT JOIN tmp_ctrl_map cm ON cm.old_code COLLATE utf8mb4_unicode_ci = jt.deviceId
  LEFT JOIN tmp_node_map nm ON nm.old_code COLLATE utf8mb4_unicode_ci = jt.nodeId
  GROUP BY r2.id
) x ON x.rid = r.id
SET r.devices = x.devs;

-- 控制日志：受控设备编码
UPDATE iot_control_log t JOIN tmp_ctrl_map m ON t.device_id = m.old_code COLLATE utf8mb4_unicode_ci SET t.device_id = m.new_code;

-- 传感器组合 sensor_codes JSON
UPDATE iot_sensor_group g
JOIN (
  SELECT g2.id AS gid, JSON_ARRAYAGG(COALESCE(m.new_code, jt.code)) AS codes
  FROM iot_sensor_group g2,
  JSON_TABLE(g2.sensor_codes, '$[*]' COLUMNS (code VARCHAR(64) PATH '$')) jt
  LEFT JOIN tmp_sensor_map m ON m.old_code COLLATE utf8mb4_unicode_ci = jt.code
  WHERE g2.is_deleted = 0
  GROUP BY g2.id
) x ON x.gid = g.id
SET g.sensor_codes = x.codes;

-- ===== 3) 改写主表编码 =====

UPDATE iot_sensor s JOIN tmp_sensor_map m ON s.id = m.id SET s.sensor_code = m.new_code;
UPDATE iot_device_ctrl d JOIN tmp_ctrl_map m ON d.id = m.id SET d.device_code = m.new_code;
UPDATE iot_node n JOIN tmp_node_map m ON n.id = m.id SET n.node_code = m.new_code;
UPDATE iot_gateway g JOIN tmp_gw_map m ON g.id = m.id SET g.gateway_code = m.new_code;

DROP TEMPORARY TABLE IF EXISTS tmp_gw_map;
DROP TEMPORARY TABLE IF EXISTS tmp_node_map;
DROP TEMPORARY TABLE IF EXISTS tmp_sensor_map;
DROP TEMPORARY TABLE IF EXISTS tmp_ctrl_map;
