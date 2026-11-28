-- 按预置字典格式化现有传感器/控制设备的大类与类型编码（一次性，Liquibase 跟踪执行）。
-- 旧编码（008 期字母编码）→ 预置数字编码（013/014）：
--   传感器：SO01→10/1001 土壤温度，SO02→11/1101 土壤水分，TH01→20/2001 空气温度，
--           TH02→21/2101 空气湿度，WS01→24/2401 风速，WS02→25/2501 风向，LQ01→51/5101 溶解氧RDO
--   设备：  F0101→21/2101 风机，I0101→40/4001 滴灌电磁阀，M0101→23/2301 保温被（三开卷帘机），
--           P0101→60/6001 水泵，W0101→90/9001 开度设备
-- 同步：传感器 unit 按新大类字典刷新；控制规则 devices JSON 的 deviceType 改为新大类编码；
--       清理字典表中的旧字母编码条目。

-- ===== 1) 传感器大类/类型 =====

UPDATE `iot_sensor`
SET sensor_class = CASE sensor_class_type
      WHEN 'SO01' THEN '10'
      WHEN 'SO02' THEN '11'
      WHEN 'TH01' THEN '20'
      WHEN 'TH02' THEN '21'
      WHEN 'WS01' THEN '24'
      WHEN 'WS02' THEN '25'
      WHEN 'LQ01' THEN '51'
      ELSE sensor_class END,
  sensor_class_type = CASE sensor_class_type
      WHEN 'SO01' THEN '1001'
      WHEN 'SO02' THEN '1101'
      WHEN 'TH01' THEN '2001'
      WHEN 'TH02' THEN '2101'
      WHEN 'WS01' THEN '2401'
      WHEN 'WS02' THEN '2501'
      WHEN 'LQ01' THEN '5101'
      ELSE sensor_class_type END
WHERE sensor_class IN ('TH', 'SO', 'WS', 'LQ', 'EC');

-- 单位按新大类字典刷新（仅受影响的预置大类）
UPDATE `iot_sensor` s
JOIN `iot_sensor_class` c ON c.`class` = s.`sensor_class`
SET s.`unit` = COALESCE(c.`unit`, s.`unit`)
WHERE s.`sensor_class` IN ('10', '11', '20', '21', '24', '25', '51');

-- ===== 2) 控制设备大类/类型 =====

UPDATE `iot_device_ctrl`
SET device_class = CASE device_class_type
      WHEN 'F0101' THEN '21'
      WHEN 'I0101' THEN '40'
      WHEN 'M0101' THEN '23'
      WHEN 'P0101' THEN '60'
      WHEN 'W0101' THEN '90'
      ELSE device_class END,
  device_class_type = CASE device_class_type
      WHEN 'F0101' THEN '2101'
      WHEN 'I0101' THEN '4001'
      WHEN 'M0101' THEN '2301'
      WHEN 'P0101' THEN '6001'
      WHEN 'W0101' THEN '9001'
      ELSE device_class_type END
WHERE device_class IN ('F01', 'P01', 'W01', 'I01', 'M01');

-- ===== 3) 控制规则 devices JSON 的 deviceType 同步为新大类编码 =====

UPDATE `iot_control_rule` r
JOIN (
  SELECT r2.id AS rid,
    JSON_ARRAYAGG(JSON_OBJECT(
      'deviceId', jt.deviceId,
      'deviceName', jt.deviceName,
      'nodeId', jt.nodeId,
      'channel', jt.channel,
      'deviceType', CASE jt.deviceType
          WHEN 'F01' THEN '21'
          WHEN 'I01' THEN '40'
          WHEN 'M01' THEN '23'
          WHEN 'P01' THEN '60'
          WHEN 'W01' THEN '90'
          ELSE jt.deviceType END)) AS devs
  FROM `iot_control_rule` r2,
  JSON_TABLE(r2.devices, '$[*]' COLUMNS (
      deviceId VARCHAR(64) PATH '$.deviceId',
      deviceName VARCHAR(128) PATH '$.deviceName',
      nodeId VARCHAR(64) PATH '$.nodeId',
      channel INT PATH '$.channel',
      deviceType VARCHAR(32) PATH '$.deviceType')) jt
  GROUP BY r2.id
) x ON x.rid = r.id
SET r.devices = x.devs;

-- ===== 4) 清理旧字母编码字典条目 =====

DELETE FROM `iot_sensor_class` WHERE `class` IN ('TH', 'SO', 'WS', 'LQ', 'EC');
DELETE FROM `iot_sensor_class_type` WHERE `class` IN ('TH', 'SO', 'WS', 'LQ', 'EC');
DELETE FROM `iot_device_class` WHERE `class` IN ('F01', 'P01', 'W01', 'I01', 'M01');
DELETE FROM `iot_device_class_type` WHERE `device_class` IN ('F01', 'P01', 'W01', 'I01', 'M01');
