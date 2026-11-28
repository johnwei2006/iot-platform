-- 生产管理页面按领域归类：views/production/{gateway,node,sensor,device}
-- 动态路由按 sys_menu.component 解析组件，这里将旧组件路径迁移到新目录。
-- 幂等：仅更新仍为旧值的记录，重复执行无副作用。

UPDATE `sys_menu`
SET `component` = 'production/gateway/gateways'
WHERE `component` = 'production/gateways';

UPDATE `sys_menu`
SET `component` = 'production/node/nodes'
WHERE `component` = 'production/nodes';

UPDATE `sys_menu`
SET `component` = 'production/node/node-detail'
WHERE `component` = 'production/node-detail';

UPDATE `sys_menu`
SET `component` = 'production/node/node-config'
WHERE `component` = 'production/node-config';

UPDATE `sys_menu`
SET `component` = 'production/sensor/sensors'
WHERE `component` = 'production/sensors';

UPDATE `sys_menu`
SET `component` = 'production/sensor/sensor-data'
WHERE `component` = 'production/sensor-data';

UPDATE `sys_menu`
SET `component` = 'production/device/control-devices'
WHERE `component` = 'production/control-devices';

UPDATE `sys_menu`
SET `component` = 'production/device/device-control'
WHERE `component` = 'production/device-control';
