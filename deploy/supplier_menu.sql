-- ========================================
-- 供应商管理 - 菜单与权限初始化脚本
-- 功能：为供应商管理页面添加数据库菜单记录及角色权限关联
-- 前置条件：t_menu 表中已存在仓库管理父菜单 (id=5)
-- ========================================

-- ========================================
-- 1. 添加页面级菜单 (menu_type=1)
-- ========================================
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, visible, status) VALUES
  (55, 5, '供应商管理', 1, '/warehouse/supplier', 'pages/warehouse/Supplier', 'warehouse:supplier', 'OfficeBuilding', 5, 1, 1);

-- ========================================
-- 2. 添加按钮级菜单 (menu_type=2)
-- 删除操作仅限 SUPER_ADMIN（后端已用 @PreAuthorize 控制）
-- ========================================
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (551, 55, '新增供应商', 2, NULL, NULL, NULL, 1, 'warehouse:supplier'),
  (552, 55, '编辑供应商', 2, NULL, NULL, NULL, 2, 'warehouse:supplier');

-- ========================================
-- 3. SUPER_ADMIN (role_id=1) 关联所有供应商菜单
-- ========================================
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES
  (1, 55), (1, 551), (1, 552);

-- ========================================
-- 4. 机构负责人 ORG_MANAGER (role_id=2) 拥有供应商管理权限
-- ========================================
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES
  (2, 55), (2, 551), (2, 552);

-- ========================================
-- 5. 仓库员 WAREHOUSE (role_id=7) 拥有供应商管理权限
-- ========================================
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES
  (7, 55), (7, 551), (7, 552);
