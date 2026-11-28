-- 控制设备下发指令字段：对齐 iot_sensor.command_read 的存储模式。
-- 开/关/设置指令为 485 Hex 格式（直接发送给 modbus），非 485 接入可空。
-- 幂等：由 master changelog 的 columnExists 前置条件保证，重复执行跳过。

ALTER TABLE `iot_device_ctrl`
  ADD COLUMN `command_on`  VARCHAR(64) DEFAULT NULL COMMENT '开指令（485 Hex格式）' AFTER `channel`,
  ADD COLUMN `command_off` VARCHAR(64) DEFAULT NULL COMMENT '关指令（485 Hex格式）' AFTER `command_on`,
  ADD COLUMN `command_set` VARCHAR(64) DEFAULT NULL COMMENT '设置指令（485 Hex格式）' AFTER `command_off`;
