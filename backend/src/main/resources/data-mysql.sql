-- ========================================
-- 养老企业管理系统 种子数据 (MySQL)
-- 使用 INSERT ... ON DUPLICATE KEY UPDATE 保证幂等性
-- ========================================

-- ========================================
-- 超级管理员用户
-- 密码: Admin123! (BCrypt加密)
-- ========================================
INSERT INTO t_user (id, username, password, real_name, status)
VALUES (1, 'admin', '$2a$10$9KVgdbkr.8b6joLYrPGHk.LdenJvau80Kjm1.pA1lRhUTNzfgLGRe', '超级管理员', 1)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  real_name = VALUES(real_name),
  status = VALUES(status);

-- ========================================
-- 默认角色
-- ========================================
INSERT INTO t_role (id, role_name, role_code, description)
VALUES
  (1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
  (2, '机构负责人', 'ORG_MANAGER', '可配置系统参数与运营看板'),
  (3, '财务人员', 'FINANCE', '缴费、凭证、报账、月结等'),
  (4, '人事专员', 'HR', '护工档案、排班、打卡管理'),
  (5, '护理主管', 'NURSING_SUPERVISOR', '护工考核、护理计划分配'),
  (6, '护工', 'CAREGIVER', '打卡、护理记录录入'),
  (7, '仓库员', 'WAREHOUSE', '物资入出库登记、库存盘点'),
  (8, '药剂员', 'PHARMACIST', '药品管理、发药登记'),
  (9, '上门服务专员', 'HOME_SERVICE', '预约受理、服务记录提交')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  role_code = VALUES(role_code),
  description = VALUES(description);

-- ========================================
-- 绑定 admin 用户到超级管理员角色
-- ========================================
INSERT INTO t_user_role (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ========================================
-- 菜单树 - 一级菜单(目录)
-- ========================================
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (1, 0, '首页', 0, '/dashboard', 'pages/Dashboard', 'HomeFilled', 1, NULL),
  (2, 0, '入住管理', 0, '/elderly', NULL, 'User', 2, NULL),
  (3, 0, '人事管理', 0, '/staff', NULL, 'UserFilled', 3, NULL),
  (4, 0, '财务管理', 0, '/finance', NULL, 'Money', 4, NULL),
  (5, 0, '仓库管理', 0, '/warehouse', NULL, 'Box', 5, NULL),
  (6, 0, '药物管理', 0, '/pharmacy', NULL, 'FirstAidKit', 6, NULL),
  (7, 0, '上门服务', 0, '/home-service', NULL, 'Van', 7, NULL),
  (8, 0, '系统管理', 0, '/system', NULL, 'Setting', 8, NULL),
  (9, 0, '报表管理', 0, '/reports', NULL, 'DataAnalysis', 9, NULL)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- ========================================
-- 菜单树 - 二级菜单(页面)
-- ========================================
-- 入住管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (21, 2, '在住老人列表', 1, '/elderly/list', 'pages/elderly/ElderlyList', NULL, 1, 'elderly:list'),
  (22, 2, '新增入住', 1, '/elderly/add', 'pages/elderly/ElderlyAdd', NULL, 2, 'elderly:add'),
  (23, 2, '退住管理', 1, '/elderly/discharge', 'pages/elderly/ElderlyDischarge', NULL, 3, 'elderly:discharge'),
  (24, 2, '转床管理', 1, '/elderly/transfer', 'pages/elderly/ElderlyTransfer', NULL, 4, 'elderly:transfer')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 人事管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (31, 3, '护工管理', 1, '/staff/list', 'pages/staff/StaffList', NULL, 1, 'staff:list'),
  (34, 3, '员工管理', 1, '/staff/employee', 'pages/staff/EmployeeList', NULL, 2, 'staff:employee'),
  (32, 3, '打卡记录', 1, '/staff/attendance', 'pages/staff/Attendance', NULL, 3, 'staff:attendance'),
  (33, 3, '排班管理', 1, '/staff/schedule', 'pages/staff/Schedule', NULL, 4, 'staff:schedule')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 财务管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (41, 4, '收支管理', 1, '/finance/payment', 'pages/finance/Payment', NULL, 1, 'finance:cashflow'),
  (42, 4, '凭证管理', 1, '/finance/voucher', 'pages/finance/Voucher', NULL, 2, 'finance:voucher'),
  (43, 4, '报账管理', 1, '/finance/reimbursement', 'pages/finance/Reimbursement', NULL, 3, 'finance:reimbursement'),
  (44, 4, '月度账单', 1, '/finance/bills', 'pages/finance/FeeBill', NULL, 4, 'finance:bill'),
(45, 4, '补贴对账', 1, '/finance/subsidy-summary', 'pages/finance/SubsidySummary', NULL, 5, 'finance:subsidy')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 报表管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (91, 9, '报表导出', 1, '/reports/export', 'pages/reports/Reports', NULL, 1, 'report:export')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 仓库管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (50, 5, '物资档案', 1, '/warehouse/materials', 'pages/warehouse/Materials', NULL, 0, 'warehouse:materials'),
  (51, 5, '入库管理', 1, '/warehouse/in', 'pages/warehouse/InventoryIn', NULL, 1, 'warehouse:in'),
  (52, 5, '出库管理', 1, '/warehouse/out', 'pages/warehouse/InventoryOut', NULL, 2, 'warehouse:out'),
  (53, 5, '库存看板', 1, '/warehouse/stock', 'pages/warehouse/Stock', NULL, 3, 'warehouse:stock'),
  (54, 5, '盘点管理', 1, '/warehouse/check', 'pages/warehouse/InventoryCheck', NULL, 4, 'warehouse:check')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 药物管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (61, 6, '药品档案', 1, '/pharmacy/drugs', 'pages/pharmacy/DrugList', NULL, 1, 'pharmacy:drugs'),
  (62, 6, '发药管理', 1, '/pharmacy/dispense', 'pages/pharmacy/Dispense', NULL, 2, 'pharmacy:dispense')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 上门服务子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (71, 7, '预约管理', 1, '/home-service/orders', 'pages/home-service/Orders', NULL, 1, 'home-service:orders'),
  (72, 7, '服务记录', 1, '/home-service/records', 'pages/home-service/Records', NULL, 2, 'home-service:records'),
  (73, 7, '服务评估', 1, '/home-service/assessment', 'pages/home-service/Assessment', NULL, 3, 'home-service:assessment')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- 系统管理子菜单
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  (81, 8, '账户管理', 1, '/system/accounts', 'pages/system/Accounts', NULL, 1, 'system:accounts'),
  (82, 8, '角色管理', 1, '/system/roles', 'pages/system/Roles', NULL, 2, 'system:roles'),
  (83, 8, '菜单管理', 1, '/system/menus', 'pages/system/Menus', NULL, 3, 'system:menus'),
  (84, 8, '系统配置', 1, '/system/config', 'pages/system/Config', NULL, 4, 'system:config'),
  (85, 8, '操作日志', 1, '/system/logs', 'pages/system/Logs', NULL, 5, 'system:logs')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- ========================================
-- 按钮级菜单 (type=2)
-- ========================================
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission)
VALUES
  -- 入住管理
  (201, 21, '查看详情', 2, NULL, NULL, NULL, 1, 'elderly:list'),
  (202, 21, '编辑档案', 2, NULL, NULL, NULL, 2, 'elderly:list'),
  (203, 21, '退住', 2, NULL, NULL, NULL, 3, 'elderly:list'),
  (204, 21, '撤销退住', 2, NULL, NULL, NULL, 4, 'elderly:list'),
  (205, 21, '转床', 2, NULL, NULL, NULL, 5, 'elderly:list'),
  (206, 21, '导入模板下载', 2, NULL, NULL, NULL, 6, 'elderly:list'),
  (207, 21, '批量导入', 2, NULL, NULL, NULL, 7, 'elderly:list'),
  (208, 21, '请假', 2, NULL, NULL, NULL, 8, 'elderly:list'),
  (209, 21, '销假', 2, NULL, NULL, NULL, 9, 'elderly:list'),
  (210, 22, '提交入住', 2, NULL, NULL, NULL, 1, 'elderly:add'),
  -- 人事管理
  (301, 31, '新增护工', 2, NULL, NULL, NULL, 1, 'staff:list'),
  (302, 31, '编辑护工', 2, NULL, NULL, NULL, 2, 'staff:list'),
  (303, 31, '删除护工', 2, NULL, NULL, NULL, 3, 'staff:list'),
  (304, 31, '离职处理', 2, NULL, NULL, NULL, 4, 'staff:list'),
  (341, 34, '新增员工', 2, NULL, NULL, NULL, 1, 'staff:employee'),
  (342, 34, '编辑员工', 2, NULL, NULL, NULL, 2, 'staff:employee'),
  (343, 34, '删除员工', 2, NULL, NULL, NULL, 3, 'staff:employee'),
  (321, 32, '补录打卡', 2, NULL, NULL, NULL, 1, 'staff:attendance'),
  (331, 33, '新增排班', 2, NULL, NULL, NULL, 1, 'staff:schedule'),
  (332, 33, '编辑排班', 2, NULL, NULL, NULL, 2, 'staff:schedule'),
  (333, 33, '删除排班', 2, NULL, NULL, NULL, 3, 'staff:schedule'),
  (334, 33, '批量排班', 2, NULL, NULL, NULL, 4, 'staff:schedule'),
  -- 财务管理
  (411, 41, '删除支出', 2, NULL, NULL, NULL, 1, 'finance:cashflow'),
  (412, 41, '删除收入', 2, NULL, NULL, NULL, 2, 'finance:cashflow'),
  (421, 42, '新增凭证', 2, NULL, NULL, NULL, 1, 'finance:voucher'),
  (422, 42, '编辑凭证', 2, NULL, NULL, NULL, 2, 'finance:voucher'),
  (423, 42, '删除凭证', 2, NULL, NULL, NULL, 3, 'finance:voucher'),
  (424, 42, '提交审核', 2, NULL, NULL, NULL, 4, 'finance:voucher'),
  (425, 42, '审核通过', 2, NULL, NULL, NULL, 5, 'finance:voucher'),
  (426, 42, '审核驳回', 2, NULL, NULL, NULL, 6, 'finance:voucher'),
  (431, 43, '创建报账单', 2, NULL, NULL, NULL, 1, 'finance:reimbursement'),
  (432, 43, '审批通过', 2, NULL, NULL, NULL, 2, 'finance:reimbursement'),
  (433, 43, '审批驳回', 2, NULL, NULL, NULL, 3, 'finance:reimbursement'),
  (434, 43, '标记已支付', 2, NULL, NULL, NULL, 4, 'finance:reimbursement'),
  (435, 43, '删除报账单', 2, NULL, NULL, NULL, 5, 'finance:reimbursement'),
  (441, 44, '生成月账单', 2, NULL, NULL, NULL, 1, 'finance:bill'),
  (442, 44, '确认月结算', 2, NULL, NULL, NULL, 2, 'finance:bill'),
  (451, 45, '新增补贴策略', 2, NULL, NULL, NULL, 1, 'system:config'),
  (452, 45, '编辑补贴策略', 2, NULL, NULL, NULL, 2, 'system:config'),
  (453, 45, '删除补贴策略', 2, NULL, NULL, NULL, 3, 'system:config'),
  (454, 45, '启用/禁用', 2, NULL, NULL, NULL, 4, 'system:config'),
  -- 仓库管理
  (501, 50, '新增物资', 2, NULL, NULL, NULL, 1, 'warehouse:materials'),
  (502, 50, '编辑物资', 2, NULL, NULL, NULL, 2, 'warehouse:materials'),
  (503, 50, '删除物资', 2, NULL, NULL, NULL, 3, 'warehouse:materials'),
  (511, 51, '入库登记', 2, NULL, NULL, NULL, 1, 'warehouse:in'),
  (521, 52, '出库登记', 2, NULL, NULL, NULL, 1, 'warehouse:out'),
  (531, 53, '查看库存', 2, NULL, NULL, NULL, 1, 'warehouse:stock'),
  (541, 54, '盘点登记', 2, NULL, NULL, NULL, 1, 'warehouse:check'),
  -- 药物管理
  (611, 61, '新增药品', 2, NULL, NULL, NULL, 1, 'pharmacy:drugs'),
  (612, 61, '编辑药品', 2, NULL, NULL, NULL, 2, 'pharmacy:drugs'),
  (613, 61, '删除药品', 2, NULL, NULL, NULL, 3, 'pharmacy:drugs'),
  (621, 62, '创建发药单', 2, NULL, NULL, NULL, 1, 'pharmacy:dispense'),
  (622, 62, '确认发药', 2, NULL, NULL, NULL, 2, 'pharmacy:dispense'),
  (623, 62, '药品批次入库', 2, NULL, NULL, NULL, 3, 'pharmacy:dispense'),
  (624, 62, '效期预警查看', 2, NULL, NULL, NULL, 4, 'pharmacy:dispense'),
  -- 上门服务
  (711, 71, '创建预约单', 2, NULL, NULL, NULL, 1, 'home-service:orders'),
  (712, 71, '更新预约单', 2, NULL, NULL, NULL, 2, 'home-service:orders'),
  (721, 72, '提交服务记录', 2, NULL, NULL, NULL, 1, 'home-service:records'),
  (731, 73, '创建评估', 2, NULL, NULL, NULL, 1, 'home-service:assessment'),
  (732, 73, '编辑评估', 2, NULL, NULL, NULL, 2, 'home-service:assessment'),
  (733, 73, '删除评估', 2, NULL, NULL, NULL, 3, 'home-service:assessment'),
  (734, 73, '提交评估', 2, NULL, NULL, NULL, 4, 'home-service:assessment'),
  (735, 73, '确认评估', 2, NULL, NULL, NULL, 5, 'home-service:assessment'),
  -- 报表管理
  (911, 91, '导出费用汇总', 2, NULL, NULL, NULL, 1, 'report:export'),
  (912, 91, '导出人员名册', 2, NULL, NULL, NULL, 2, 'report:export'),
  (913, 91, '导出考勤报表', 2, NULL, NULL, NULL, 3, 'report:export'),
  (914, 91, '导出物资消耗', 2, NULL, NULL, NULL, 4, 'report:export'),
  -- 系统管理
  (811, 81, '创建用户', 2, NULL, NULL, NULL, 1, 'system:accounts'),
  (812, 81, '编辑用户', 2, NULL, NULL, NULL, 2, 'system:accounts'),
  (813, 81, '删除用户', 2, NULL, NULL, NULL, 3, 'system:accounts'),
  (814, 81, '重置密码', 2, NULL, NULL, NULL, 4, 'system:accounts'),
  (815, 81, '启用/停用', 2, NULL, NULL, NULL, 5, 'system:accounts'),
  (821, 82, '创建角色', 2, NULL, NULL, NULL, 1, 'system:roles'),
  (822, 82, '编辑角色', 2, NULL, NULL, NULL, 2, 'system:roles'),
  (823, 82, '删除角色', 2, NULL, NULL, NULL, 3, 'system:roles'),
  (831, 83, '创建菜单', 2, NULL, NULL, NULL, 1, 'system:menus'),
  (832, 83, '编辑菜单', 2, NULL, NULL, NULL, 2, 'system:menus'),
  (833, 83, '删除菜单', 2, NULL, NULL, NULL, 3, 'system:menus'),
  (841, 84, '更新配置', 2, NULL, NULL, NULL, 1, 'system:config'),
  (851, 85, '查看日志', 2, NULL, NULL, NULL, 1, 'system:logs')
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  permission = VALUES(permission);

-- ========================================
-- 超管角色关联所有菜单
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- ========================================
-- 其他角色默认菜单关联
-- ========================================
-- 机构负责人 ORG_MANAGER (role_id=2)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 21), (2, 22), (2, 23), (2, 24),
(2, 201), (2, 202), (2, 203), (2, 204), (2, 205), (2, 206), (2, 207), (2, 208), (2, 209), (2, 210),
(2, 3), (2, 31), (2, 34), (2, 32), (2, 33),
(2, 301), (2, 302), (2, 303), (2, 304), (2, 341), (2, 342), (2, 343), (2, 321), (2, 331), (2, 332), (2, 333), (2, 334),
(2, 4), (2, 41), (2, 42), (2, 43), (2, 44), (2, 45),
(2, 411), (2, 412), (2, 421), (2, 422), (2, 423), (2, 424), (2, 425), (2, 426),
(2, 431), (2, 432), (2, 433), (2, 434), (2, 435), (2, 441), (2, 442), (2, 451), (2, 452), (2, 453), (2, 454),
(2, 5), (2, 50), (2, 51), (2, 52), (2, 53), (2, 54),
(2, 501), (2, 502), (2, 503), (2, 511), (2, 521), (2, 531), (2, 541),
(2, 6), (2, 61), (2, 62),
(2, 611), (2, 612), (2, 613), (2, 621), (2, 622), (2, 623), (2, 624),
(2, 7), (2, 71), (2, 72), (2, 73),
(2, 711), (2, 712), (2, 721), (2, 731), (2, 732), (2, 733), (2, 734), (2, 735),
(2, 8), (2, 81), (2, 82), (2, 83), (2, 84), (2, 85),
(2, 811), (2, 812), (2, 813), (2, 814), (2, 815), (2, 821), (2, 822), (2, 823), (2, 831), (2, 832), (2, 833), (2, 841), (2, 851),
(2, 9), (2, 91), (2, 911), (2, 912), (2, 913), (2, 914)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 财务人员 FINANCE (role_id=3)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 2), (3, 21), (3, 201), (3, 202), (3, 206), (3, 207), (3, 208), (3, 209),
(3, 4), (3, 41), (3, 42), (3, 43), (3, 44), (3, 45),
(3, 411), (3, 412), (3, 421), (3, 422), (3, 423), (3, 424), (3, 425), (3, 426),
(3, 431), (3, 432), (3, 433), (3, 434), (3, 435), (3, 441), (3, 442), (3, 451), (3, 452), (3, 453), (3, 454),
(3, 9), (3, 91), (3, 911), (3, 912), (3, 913), (3, 914)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 人事专员 HR (role_id=4)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(4, 1), (4, 2), (4, 21), (4, 201), (4, 202), (4, 206), (4, 207), (4, 208), (4, 209),
(4, 3), (4, 31), (4, 34), (4, 32), (4, 33),
(4, 301), (4, 302), (4, 303), (4, 304), (4, 341), (4, 342), (4, 343), (4, 321), (4, 331), (4, 332), (4, 333), (4, 334),
(4, 9), (4, 91), (4, 911), (4, 912), (4, 913), (4, 914)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 护理主管 NURSING_SUPERVISOR (role_id=5)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(5, 1), (5, 2), (5, 21), (5, 22), (5, 23), (5, 24),
(5, 201), (5, 202), (5, 203), (5, 204), (5, 205), (5, 206), (5, 207), (5, 208), (5, 209), (5, 210),
(5, 3), (5, 31), (5, 32), (5, 301), (5, 302), (5, 303), (5, 304), (5, 321),
(5, 9), (5, 91), (5, 911), (5, 912), (5, 913), (5, 914)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 护工 CAREGIVER (role_id=6)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(6, 1), (6, 2), (6, 21), (6, 201), (6, 202), (6, 206), (6, 207), (6, 208), (6, 209),
(6, 3), (6, 32), (6, 321)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 仓库员 WAREHOUSE (role_id=7)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(7, 1), (7, 5), (7, 50), (7, 51), (7, 52), (7, 53), (7, 54),
(7, 501), (7, 502), (7, 503), (7, 511), (7, 521), (7, 531), (7, 541),
(7, 9), (7, 91), (7, 911), (7, 912), (7, 913), (7, 914)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 药剂员 PHARMACIST (role_id=8)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(8, 1), (8, 6), (8, 61), (8, 62),
(8, 611), (8, 612), (8, 613), (8, 621), (8, 622), (8, 623), (8, 624),
(8, 9), (8, 91), (8, 911), (8, 912), (8, 913), (8, 914)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 上门服务专员 HOME_SERVICE (role_id=9)
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(9, 1), (9, 2), (9, 21), (9, 201), (9, 202), (9, 206), (9, 207), (9, 208), (9, 209),
(9, 7), (9, 71), (9, 72), (9, 73),
(9, 711), (9, 712), (9, 721), (9, 731), (9, 732), (9, 733), (9, 734), (9, 735)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- ========================================
-- 默认系统配置
-- ========================================
INSERT INTO t_system_config (id, config_key, config_value, config_type, description)
VALUES
  (1, 'org_name', '养老院', 'STRING', '机构名称'),
  (2, 'org_address', '', 'STRING', '机构地址'),
  (3, 'org_phone', '', 'STRING', '联系方式'),
  (4, 'short_term_daily_rate', '180', 'NUMBER', '短期入住日费率(元)'),
  (5, 'fee_warning_days', '7', 'NUMBER', '费用预警天数'),
  (6, 'drug_expiry_warning_days', '30', 'NUMBER', '药品近效期预警天数'),
  (10, 'contract_expiry_warning_days', '30', 'NUMBER', '合同到期预警天数'),
  (7, 'age_warning_threshold', '60', 'NUMBER', '年龄标红阈值'),
  (8, 'enable_long_care', 'true', 'BOOLEAN', '长护险策略开关'),
  (9, 'enable_coupon', 'true', 'BOOLEAN', '消费券策略开关')
ON DUPLICATE KEY UPDATE
  config_key = VALUES(config_key),
  config_value = VALUES(config_value),
  config_type = VALUES(config_type),
  description = VALUES(description);

-- ========================================
-- 默认床位数据 (1栋 1F/2F)
-- ========================================
INSERT INTO t_bed (id, building, floor, room_number, bed_number, status)
VALUES
  (1, '1栋', '1F', '101', '101-1', 0),
  (2, '1栋', '1F', '101', '101-2', 0),
  (3, '1栋', '1F', '102', '102-1', 0),
  (4, '1栋', '1F', '102', '102-2', 0),
  (5, '1栋', '1F', '103', '103-1', 0),
  (6, '1栋', '1F', '103', '103-2', 0),
  (7, '1栋', '2F', '201', '201-1', 0),
  (8, '1栋', '2F', '201', '201-2', 0),
  (9, '1栋', '2F', '202', '202-1', 0),
  (10, '1栋', '2F', '202', '202-2', 0)
ON DUPLICATE KEY UPDATE
  building = VALUES(building),
  floor = VALUES(floor),
  room_number = VALUES(room_number),
  bed_number = VALUES(bed_number),
  status = VALUES(status);

-- ========================================
-- 示例数据：用于 Dashboard 欠费提醒/铃铛演示（可删除）
-- ========================================
INSERT INTO t_elderly (id, unique_no, name, id_card, gender, birth_date, age, admission_date, bed_id, category, enable_long_care, enable_coupon, contract_monthly_fee, status)
VALUES (1, '2026040001', '张大爷', 'ENC(110101199001011234)', 1, '1990-01-01', 66, CURDATE(), 1, 'SOCIAL', 0, 0, 5400.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  unique_no = VALUES(unique_no),
  name = VALUES(name),
  id_card = VALUES(id_card),
  gender = VALUES(gender),
  birth_date = VALUES(birth_date),
  age = VALUES(age),
  admission_date = VALUES(admission_date),
  bed_id = VALUES(bed_id),
  category = VALUES(category),
  enable_long_care = VALUES(enable_long_care),
  enable_coupon = VALUES(enable_coupon),
  contract_monthly_fee = VALUES(contract_monthly_fee),
  status = VALUES(status);

INSERT INTO t_fee_account (id, elderly_id, balance, total_charged, total_consumed, warning_status)
VALUES (1, 1, 300.00, 300.00, 0.00, 1)
ON DUPLICATE KEY UPDATE
  balance = VALUES(balance),
  total_charged = VALUES(total_charged),
  total_consumed = VALUES(total_consumed),
  warning_status = VALUES(warning_status);

INSERT INTO t_reimbursement (id, applicant_id, amount, reason, status)
VALUES (1, 1, 120.50, '示例：采购消毒用品报账', 'PENDING')
ON DUPLICATE KEY UPDATE
  applicant_id = VALUES(applicant_id),
  amount = VALUES(amount),
  reason = VALUES(reason),
  status = VALUES(status);
