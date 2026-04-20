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
-- 超管角色关联所有菜单
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu
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
