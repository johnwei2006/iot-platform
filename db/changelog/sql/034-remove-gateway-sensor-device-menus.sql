-- 删除「网关管理」下的「网关传感器」「网关设备」菜单及其按钮权限，幂等。
-- 先清理角色/产品授权再删菜单，避免残留孤儿授权。

UPDATE `sys_role` r
SET r.`menu_ids` = (
  SELECT COALESCE(JSON_ARRAYAGG(jt.val), JSON_ARRAY())
  FROM JSON_TABLE(r.`menu_ids`, '$[*]' COLUMNS (val VARCHAR(36) PATH '$')) jt
  WHERE jt.val IS NULL
     OR jt.val NOT IN (
       SELECT m.id FROM `sys_menu` m
       WHERE m.`path` IN ('/production/gateway/sensors', '/production/gateway/devices')
          OR m.`perm` LIKE 'gateway:sensor:%'
          OR m.`perm` LIKE 'gateway:device:%'
          OR m.`parent_id` IN (
            SELECT p.id FROM `sys_menu` p
            WHERE p.`path` IN ('/production/gateway/sensors', '/production/gateway/devices')
          )
     )
);

DELETE pm FROM `sys_product_menu` pm
JOIN `sys_menu` m ON m.id COLLATE utf8mb4_unicode_ci = pm.`menu_id` COLLATE utf8mb4_unicode_ci
WHERE m.`path` IN ('/production/gateway/sensors', '/production/gateway/devices')
   OR m.`perm` LIKE 'gateway:sensor:%'
   OR m.`perm` LIKE 'gateway:device:%'
   OR m.`parent_id` IN (
     SELECT p.id FROM `sys_menu` p
     WHERE p.`path` IN ('/production/gateway/sensors', '/production/gateway/devices')
   );

DELETE c FROM `sys_menu` c
JOIN `sys_menu` p ON c.`parent_id` COLLATE utf8mb4_unicode_ci = p.id COLLATE utf8mb4_unicode_ci
WHERE p.`path` IN ('/production/gateway/sensors', '/production/gateway/devices');

DELETE FROM `sys_menu`
WHERE `path` IN ('/production/gateway/sensors', '/production/gateway/devices')
   OR `perm` LIKE 'gateway:sensor:%'
   OR `perm` LIKE 'gateway:device:%';
