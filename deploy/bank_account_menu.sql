-- ========================================
-- 银行账户/五保拨付 菜单及角色权限脚本
-- ========================================

-- 财务管理(parent_id=4)下新增页面级菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission, visible, status) VALUES
  (46, 4, '银行账户管理', 1, '/finance/bank-account', 'pages/finance/BankAccount', NULL, 6, 'finance:bank-account', 1, 1),
  (47, 4, '五保拨付管理', 1, '/finance/wubao-allocate', 'pages/finance/WubaoAllocate', NULL, 7, 'finance:wubao-allocate', 1, 1);

-- 为 SUPER_ADMIN(1)、机构负责人/ADMIN(2)、财务人员(3) 分配新菜单权限
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES
  (1, 46), (2, 46), (3, 46),
  (1, 47), (2, 47), (3, 47);
