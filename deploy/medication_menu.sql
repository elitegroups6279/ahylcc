-- 用药管理菜单（药物管理子菜单，parent_id=6）
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission, visible, status)
VALUES 
  (63, 6, '今日用药', 1, '/pharmacy/medication-dashboard', 'pages/pharmacy/MedicationDashboard', 'Syringe', 3, 'pharmacy:medication', 1, 1),
  (64, 6, '用药计划', 1, '/pharmacy/medication-plans', 'pages/pharmacy/MedicationPlan', 'Calendar', 4, 'pharmacy:medication', 1, 1);

-- 超级管理员角色关联
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES (1, 63), (1, 64);

-- 如果有药剂员角色 (假设 role_id=8)
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES (8, 63), (8, 64);
