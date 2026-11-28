-- 节点管理下新增二级菜单「节点详情」（详情页由生产节点列表"详情"按钮携带 id 进入；
-- 菜单本身用于权限/导航注册，未带 id 直接访问时页面显示引导提示）。
-- 幂等：INSERT IGNORE；产品-菜单授权跟随「生产节点」的既有授权。

INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perm`, `icon`, `sort`, `status`, `created_at`)
VALUES
  ('e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f602', 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f601', '节点详情', 'MENU',
   '/production/node-detail', 'production/node-detail', NULL, NULL, '20', 'ENABLED', NOW());

INSERT IGNORE INTO `sys_product_menu` (`product_id`, `menu_id`)
SELECT pm.product_id, 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f602'
FROM `sys_product_menu` pm
WHERE pm.menu_id = 'd34d2bad-4004-4177-a597-e3da5ce89fcf';
