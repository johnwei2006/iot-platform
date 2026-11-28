-- 修复产品-菜单授权回填：
-- 029/030 的 INSERT IGNORE ... SELECT 未提供 sys_product_menu.id（NOT NULL 主键、无默认值），
-- 在非严格 sql_mode 下静默写入 id='' 或整行跳过。
-- 本变更集：1) 修复空 id 行；2) 以 NOT EXISTS 幂等回填「节点管理」「节点详情」的产品授权。

UPDATE `sys_product_menu` SET `id` = REPLACE(UUID(), '-', '') WHERE `id` = '' OR `id` IS NULL;

INSERT IGNORE INTO `sys_product_menu` (`id`, `product_id`, `menu_id`, `created_at`)
SELECT REPLACE(UUID(), '-', ''), pm.product_id, map.mid, NOW()
FROM `sys_product_menu` pm
JOIN (
  SELECT 'd34d2bad-4004-4177-a597-e3da5ce89fcf' AS src, 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f601' AS mid
  UNION ALL
  SELECT 'd34d2bad-4004-4177-a597-e3da5ce89fcf', 'e1a2b3c4-d5e6-4f70-8a91-b2c3d4e5f602'
) map ON pm.menu_id = map.src
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_product_menu` x
  WHERE x.product_id = pm.product_id AND x.menu_id = map.mid
);
