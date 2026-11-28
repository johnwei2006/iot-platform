-- 菜单改造：「节点详情」更名为「节点配置」，路径与组件同步切换到 node-config 页面。
-- 配置页自带节点搜索/列表，不再依赖路由参数；未选节点时页面显示引导提示。
-- 幂等：按固定 id 与旧名称条件更新。

UPDATE `sys_menu`
SET `name` = '节点配置',
    `path` = '/production/node-config',
    `component` = 'production/node-config'
WHERE `id` = 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f602'
  AND `name` = '节点详情';
