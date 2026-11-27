-- 网关不再保存 node_id：节点通过 iot_node.gateway_code 反向挂靠网关。
-- 幂等：列不存在时跳过（避免已有手工删列的库执行失败）。
SET @drop_gateway_node := (
  SELECT IF(COUNT(*) > 0,
           'ALTER TABLE `iot_gateway` DROP COLUMN `node_id`',
           'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'iot_gateway'
    AND COLUMN_NAME = 'node_id'
);
PREPARE stmt FROM @drop_gateway_node;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
