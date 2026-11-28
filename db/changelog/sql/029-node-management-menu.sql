-- 菜单调整：新增一级菜单「节点管理」，二级菜单「生产节点」移至其下（保留原路由与组件）。
-- 幂等：目录用 INSERT IGNORE（主键冲突跳过）；产品-菜单授权跟随生产节点的既有授权。

INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `name`, `type`, `path`, `component`, `perm`, `icon`, `sort`, `status`, `created_at`)
VALUES
  ('e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f601', NULL, '节点管理', 'DIR', '/node', NULL, NULL, NULL, '35', 'ENABLED', NOW());

UPDATE `sys_menu`
SET `parent_id` = 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f601', `sort` = '10'
WHERE `id` = 'd34d2bad-4004-4177-a597-e3da5ce89fcf' AND `name` = '生产节点';

-- 凡授权过「生产节点」的产品，同步授权「节点管理」目录
INSERT IGNORE INTO `sys_product_menu` (`product_id`, `menu_id`)
SELECT pm.product_id, 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f601'
FROM `sys_product_menu` pm
WHERE pm.menu_id = 'd34d2bad-4004-4177-a597-e3da5ce89fcf';
