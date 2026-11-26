ALTER TABLE `iot_gateway` ADD COLUMN `area_id` VARCHAR(36) DEFAULT NULL COMMENT '安装区域ID（iot_area.id）';
ALTER TABLE `iot_gateway` ADD INDEX `idx_gateway_area` (`area_id`);
ALTER TABLE `iot_node` ADD COLUMN `area_id` VARCHAR(36) DEFAULT NULL COMMENT '安装区域ID（iot_area.id）';
ALTER TABLE `iot_node` ADD INDEX `idx_node_area` (`area_id`);
ALTER TABLE `iot_sensor` ADD COLUMN `area_id` VARCHAR(36) DEFAULT NULL COMMENT '关联区域ID（iot_area.id）';
ALTER TABLE `iot_sensor` ADD INDEX `idx_sensor_area` (`area_id`);
ALTER TABLE `iot_device_ctrl` ADD COLUMN `area_id` VARCHAR(36) DEFAULT NULL COMMENT '关联区域ID（iot_area.id）';
ALTER TABLE `iot_device_ctrl` ADD INDEX `idx_device_ctrl_area` (`area_id`);
