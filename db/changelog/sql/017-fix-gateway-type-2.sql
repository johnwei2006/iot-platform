-- 网关类型 2 修正为“工控网关”（原误写为“公开”），仅改 COMMENT，不改数据。
ALTER TABLE `iot_gateway`
  MODIFY COLUMN `gateway_type` TINYINT(4) DEFAULT NULL COMMENT '网关类型：1-4G，2-工控网关，3-5G，4-其它';
