-- 菜单位置修正：节点管理目录放到「生产管理」下，与「网关管理」并列；
-- 生产节点更名为「节点生产」；节点详情保持其下。
-- 幂等：按固定 id/名称条件更新。

UPDATE `sys_menu`
SET `parent_id` = '5494eccf-2aa1-4503-8d4f-f1feeb7c10fb',
    `path` = '/production/node-manage',
    `sort` = '26'
WHERE `id` = 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f601';

UPDATE `sys_menu`
SET `name` = '节点生产'
WHERE `id` = 'd34d2bad-4004-4177-a597-e3da5ce89fcf' AND `name` = '生产节点';
