-- 定时控制规则的“生效/失效日期”复用 start_time/end_time 字段（YYYY-MM-DD，10 字符）；
-- 条件规则仍在这两个字段存 HH:mm 生效时段（5 字符）。原 varchar(8) 存不下日期，
-- 扩展为 varchar(10) 同时兼容两种格式。

ALTER TABLE `iot_control_rule`
  MODIFY COLUMN `start_time` VARCHAR(10) DEFAULT NULL COMMENT '定时规则生效日期 YYYY-MM-DD；条件规则生效时段 HH:mm',
  MODIFY COLUMN `end_time` VARCHAR(10) DEFAULT NULL COMMENT '定时规则失效日期 YYYY-MM-DD；条件规则生效时段 HH:mm';
