-- 网关管理菜单迁移：新建 DIR，挂载 6 个子菜单与按钮权限；幂等。
-- 菜单与权限的最终权威仍为 admin-service DataInitializer，本迁移保证旧库升级后立即可用。

SET @prod_id = (SELECT id FROM sys_menu WHERE path = '/production' LIMIT 1);
SET @gw_dir_id = NULL;

SELECT id INTO @gw_dir_id FROM sys_menu WHERE path = '/production/gateway-manage' LIMIT 1;
SET @gw_dir_id = IFNULL(@gw_dir_id, UUID());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perm, icon, sort, status, created_at)
SELECT @gw_dir_id, @prod_id, '网关管理', 'DIR', '/production/gateway-manage', NULL, NULL, 'Cpu', '25', 'ENABLED', NOW(6)
WHERE @prod_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/production/gateway-manage');

-- 迁移原生产网关为网关管理子目录
UPDATE sys_menu SET parent_id = @gw_dir_id, name = '生产网关' WHERE path = '/production/gateways';

SET @menu_count = 0;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perm, icon, sort, status, created_at)
SELECT UUID(), @gw_dir_id, n.name, 'MENU', n.path, n.component, n.perm, n.icon, n.sort, 'ENABLED', NOW(6)
FROM (
  SELECT '网关信息及配置' AS name, '/production/gateway/info-config' AS path, 'production/gateway/info-config' AS component, 'gateway:info:page' AS perm, 'Setting' AS icon, '20' AS sort
  UNION ALL SELECT '网关节点', '/production/gateway/nodes', 'production/gateway/nodes', 'gateway:node:page', 'Grid', '30'
  UNION ALL SELECT '网关传感器', '/production/gateway/sensors', 'production/gateway/sensors', 'gateway:sensor:page', 'Sunny', '40'
  UNION ALL SELECT '网关设备', '/production/gateway/devices', 'production/gateway/devices', 'gateway:device:page', 'Monitor', '50'
  UNION ALL SELECT '网关任务', '/production/gateway/tasks', 'production/gateway/tasks', 'gateway:task:page', 'List', '60'
) n
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.path = n.path);

-- 按钮权限
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perm, icon, sort, status, created_at)
SELECT UUID(), m.id, CONCAT(m.name, b.name), 'BUTTON', NULL, NULL, CONCAT(b.p, b.s), NULL, '0', 'ENABLED', NOW(6)
FROM sys_menu m
JOIN (
  SELECT 'gateway:info' AS p, '查看' AS name, ':view' AS s
  UNION ALL SELECT 'gateway:info', '下发配置', ':config'
  UNION ALL SELECT 'gateway:info', '远程重启', ':reboot'
  UNION ALL SELECT 'gateway:node', '查看', ':view'
  UNION ALL SELECT 'gateway:node', '下发配置', ':config'
  UNION ALL SELECT 'gateway:sensor', '查看', ':view'
  UNION ALL SELECT 'gateway:sensor', '下发配置', ':config'
  UNION ALL SELECT 'gateway:device', '查看', ':view'
  UNION ALL SELECT 'gateway:device', '下发配置', ':config'
  UNION ALL SELECT 'gateway:task', '查看', ':view'
  UNION ALL SELECT 'gateway:task', '下发配置', ':config'
  UNION ALL SELECT 'gateway:task', '远程重启', ':reboot'
) b ON m.perm = CONCAT(b.p, ':page') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x
  WHERE x.perm = CONCAT(b.p, b.s) COLLATE utf8mb4_unicode_ci);

-- 产品菜单绑定由 admin-service DataInitializer 在启动时按菜单树幂等完成，
-- 这里不做跨表 INSERT ... SELECT，避免历史库列 collation 不一致导致迁移失败。
