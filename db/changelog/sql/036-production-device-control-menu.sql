-- 生产管理下新增二级菜单「设备控制」：按节点查看控制设备并一键控制。
-- 幂等：按 path 判重；产品授权跟随「生产控制设备」既有授权，并兜底绑定 PRODUCTION 产品。
-- sys_menu 使用 utf8mb4_0900_ai_ci，产品/授权表使用 utf8mb4_unicode_ci，跨表 id 比较显式统一排序规则。

INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perm`, `icon`, `sort`, `status`, `created_at`)
SELECT 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f604', p.id, '设备控制', 'MENU',
       '/production/device-control', 'production/device-control', 'production:device_control:page', 'SwitchButton', '60', 'ENABLED', NOW()
FROM `sys_menu` p
WHERE p.`path` = '/production'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` m WHERE m.`path` = '/production/device-control');

INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perm`, `icon`, `sort`, `status`, `created_at`)
SELECT 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f605', m.id, '一键控制', 'BUTTON',
       NULL, NULL, 'production:device_control:issue', NULL, '0', 'ENABLED', NOW()
FROM `sys_menu` m
WHERE m.`path` = '/production/device-control'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` b WHERE b.`perm` = 'production:device_control:issue');

INSERT IGNORE INTO `sys_product_menu` (`id`, `product_id`, `menu_id`, `created_at`)
SELECT REPLACE(UUID(), '-', ''), pm.product_id, m.id COLLATE utf8mb4_unicode_ci, NOW()
FROM `sys_product_menu` pm
JOIN `sys_menu` src ON src.id COLLATE utf8mb4_unicode_ci = pm.menu_id
                   AND src.`path` = '/production/control-devices'
JOIN `sys_menu` m ON m.`path` = '/production/device-control'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_product_menu` x
  WHERE x.product_id = pm.product_id
    AND x.menu_id = m.id COLLATE utf8mb4_unicode_ci
);

INSERT IGNORE INTO `sys_product_menu` (`id`, `product_id`, `menu_id`, `created_at`)
SELECT REPLACE(UUID(), '-', ''), p.id, m.id COLLATE utf8mb4_unicode_ci, NOW()
FROM `sys_product` p
JOIN `sys_menu` m ON m.`path` = '/production/device-control'
WHERE p.`code` = 'PRODUCTION'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_product_menu` x
    WHERE x.product_id = p.id
      AND x.menu_id = m.id COLLATE utf8mb4_unicode_ci
  );
