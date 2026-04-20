-- ========================================
-- 养老企业管理系统 种子数据 (H2 MySQL兼容模式)
-- 使用 MERGE INTO 保证幂等性
-- ========================================

-- ========================================
-- 超级管理员用户
-- 密码: Admin123! (BCrypt加密)
-- ========================================
MERGE INTO t_user (id, username, password, real_name, status) KEY(id) VALUES
(1, 'admin', '$2a$10$EOTE5F.un7nIhkTuJr8DRuJWl8JjpoO77a.URAqQztsC9S/rTIQ2a', '超级管理员', 1);

-- ========================================
-- 默认角色
-- ========================================
MERGE INTO t_role (id, role_name, role_code, description) KEY(id) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
(2, '机构负责人', 'ORG_MANAGER', '可配置系统参数与运营看板'),
(3, '财务人员', 'FINANCE', '缴费、凭证、报账、月结等'),
(4, '人事专员', 'HR', '护工档案、排班、打卡管理'),
(5, '护理主管', 'NURSING_SUPERVISOR', '护工考核、护理计划分配'),
(6, '护工', 'CAREGIVER', '打卡、护理记录录入'),
(7, '仓库员', 'WAREHOUSE', '物资入出库登记、库存盘点'),
(8, '药剂员', 'PHARMACIST', '药品管理、发药登记'),
(9, '上门服务专员', 'HOME_SERVICE', '预约受理、服务记录提交');

-- ========================================
-- 绑定 admin 用户到超级管理员角色
-- ========================================
MERGE INTO t_user_role (id, user_id, role_id) KEY(id) VALUES
(1, 1, 1);

-- ========================================
-- 菜单树 - 一级菜单(目录)
-- ========================================
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(1, 0, '首页', 0, '/dashboard', 'pages/Dashboard', 'HomeFilled', 1, NULL),
(2, 0, '入住管理', 0, '/elderly', NULL, 'User', 2, NULL),
(3, 0, '人事管理', 0, '/staff', NULL, 'UserFilled', 3, NULL),
(4, 0, '财务管理', 0, '/finance', NULL, 'Money', 4, NULL),
(5, 0, '仓库管理', 0, '/warehouse', NULL, 'Box', 5, NULL),
(6, 0, '药物管理', 0, '/pharmacy', NULL, 'FirstAidKit', 6, NULL),
(7, 0, '上门服务', 0, '/home-service', NULL, 'Van', 7, NULL),
(8, 0, '系统管理', 0, '/system', NULL, 'Setting', 8, NULL),
(9, 0, '报表管理', 0, '/reports', NULL, 'DataAnalysis', 9, NULL);

-- ========================================
-- 菜单树 - 二级菜单(页面)
-- ========================================
-- 入住管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(21, 2, '在住老人列表', 1, '/elderly/list', 'pages/elderly/ElderlyList', NULL, 1, 'elderly:list'),
(22, 2, '新增入住', 1, '/elderly/add', 'pages/elderly/ElderlyAdd', NULL, 2, 'elderly:add'),
(23, 2, '退住管理', 1, '/elderly/discharge', 'pages/elderly/ElderlyDischarge', NULL, 3, 'elderly:discharge'),
(24, 2, '转床管理', 1, '/elderly/transfer', 'pages/elderly/ElderlyTransfer', NULL, 4, 'elderly:transfer');

-- 人事管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(31, 3, '护工管理', 1, '/staff/list', 'pages/staff/StaffList', NULL, 1, 'staff:list'),
(32, 3, '打卡记录', 1, '/staff/attendance', 'pages/staff/Attendance', NULL, 2, 'staff:attendance'),
(33, 3, '排班管理', 1, '/staff/schedule', 'pages/staff/Schedule', NULL, 3, 'staff:schedule');

-- 财务管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(41, 4, '缴费管理', 1, '/finance/payment', 'pages/finance/Payment', NULL, 1, 'finance:payment'),
(42, 4, '凭证管理', 1, '/finance/voucher', 'pages/finance/Voucher', NULL, 2, 'finance:voucher'),
(43, 4, '报账管理', 1, '/finance/reimbursement', 'pages/finance/Reimbursement', NULL, 3, 'finance:reimbursement'),
(44, 4, '月度账单', 1, '/finance/bills', 'pages/finance/FeeBill', NULL, 4, 'finance:bill'),
(45, 4, '补贴对账', 1, '/finance/subsidy-summary', 'pages/finance/SubsidySummary', NULL, 5, 'finance:subsidy');

-- 报表管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(91, 9, '报表导出', 1, '/reports/export', 'pages/reports/Reports', NULL, 1, 'report:export');

-- 仓库管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(50, 5, '物资档案', 1, '/warehouse/materials', 'pages/warehouse/Materials', NULL, 0, 'warehouse:materials'),
(51, 5, '入库管理', 1, '/warehouse/in', 'pages/warehouse/InventoryIn', NULL, 1, 'warehouse:in'),
(52, 5, '出库管理', 1, '/warehouse/out', 'pages/warehouse/InventoryOut', NULL, 2, 'warehouse:out'),
(53, 5, '库存看板', 1, '/warehouse/stock', 'pages/warehouse/Stock', NULL, 3, 'warehouse:stock'),
(54, 5, '盘点管理', 1, '/warehouse/check', 'pages/warehouse/InventoryCheck', NULL, 4, 'warehouse:check');

-- 药物管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(61, 6, '药品档案', 1, '/pharmacy/drugs', 'pages/pharmacy/DrugList', NULL, 1, 'pharmacy:drugs'),
(62, 6, '发药管理', 1, '/pharmacy/dispense', 'pages/pharmacy/Dispense', NULL, 2, 'pharmacy:dispense');

-- 上门服务子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(71, 7, '预约管理', 1, '/home-service/orders', 'pages/home-service/Orders', NULL, 1, 'home-service:orders'),
(72, 7, '服务记录', 1, '/home-service/records', 'pages/home-service/Records', NULL, 2, 'home-service:records');

-- 系统管理子菜单
MERGE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) KEY(id) VALUES
(81, 8, '账户管理', 1, '/system/accounts', 'pages/system/Accounts', NULL, 1, 'system:accounts'),
(82, 8, '角色管理', 1, '/system/roles', 'pages/system/Roles', NULL, 2, 'system:roles'),
(83, 8, '菜单管理', 1, '/system/menus', 'pages/system/Menus', NULL, 3, 'system:menus'),
(84, 8, '系统配置', 1, '/system/config', 'pages/system/Config', NULL, 4, 'system:config'),
(85, 8, '操作日志', 1, '/system/logs', 'pages/system/Logs', NULL, 5, 'system:logs');

-- ========================================
-- 超管角色关联所有菜单 (使用子查询方式)
-- ========================================
DELETE FROM t_role_menu WHERE role_id = 1;
INSERT INTO t_role_menu (role_id, menu_id) SELECT 1, id FROM t_menu;

-- ========================================
-- 默认系统配置
-- ========================================
MERGE INTO t_system_config (id, config_key, config_value, config_type, description) KEY(id) VALUES
(1, 'org_name', '养老院', 'STRING', '机构名称'),
(2, 'org_address', '', 'STRING', '机构地址'),
(3, 'org_phone', '', 'STRING', '联系方式'),
(4, 'short_term_daily_rate', '180', 'NUMBER', '短期入住日费率(元)'),
(5, 'fee_warning_days', '7', 'NUMBER', '费用预警天数'),
(6, 'drug_expiry_warning_days', '30', 'NUMBER', '药品近效期预警天数'),
(10, 'contract_expiry_warning_days', '30', 'NUMBER', '合同到期预警天数'),
(7, 'age_warning_threshold', '60', 'NUMBER', '年龄标红阈值'),
(8, 'enable_long_care', 'true', 'BOOLEAN', '长护险策略开关'),
(9, 'enable_coupon', 'true', 'BOOLEAN', '消费券策略开关');

-- ========================================
-- 默认床位数据 (1栋 1F/2F)
-- ========================================
MERGE INTO t_bed (id, building, floor, room_number, bed_number, status) KEY(id) VALUES
(1, '1栋', '1F', '101', '101-1', 0),
(2, '1栋', '1F', '101', '101-2', 0),
(3, '1栋', '1F', '102', '102-1', 0),
(4, '1栋', '1F', '102', '102-2', 0),
(5, '1栋', '1F', '103', '103-1', 0),
(6, '1栋', '1F', '103', '103-2', 0),
(7, '1栋', '2F', '201', '201-1', 0),
(8, '1栋', '2F', '201', '201-2', 0),
(9, '1栋', '2F', '202', '202-1', 0),
(10, '1栋', '2F', '202', '202-2', 0);

-- ========================================
-- 老人请假记录表（H2兼容版本）
-- ========================================
CREATE TABLE IF NOT EXISTS t_elderly_leave (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  reason VARCHAR(200),
  status VARCHAR(20) DEFAULT 'ON_LEAVE',
  return_date DATE,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 补贴政策配置表（H2兼容版本）
-- ========================================
CREATE TABLE IF NOT EXISTS t_subsidy_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  policy_code VARCHAR(50) NOT NULL,
  policy_name VARCHAR(100) NOT NULL,
  category VARCHAR(20),
  disability_level VARCHAR(20),
  calc_type VARCHAR(20) NOT NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  threshold_amount DECIMAL(12,2),
  deduct_amount DECIMAL(12,2),
  pay_target VARCHAR(20) DEFAULT 'ORG',
  min_stay_days INT,
  effective_date DATE NOT NULL,
  expire_date DATE,
  enabled TINYINT DEFAULT 1,
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 补贴政策预置数据
-- ========================================
INSERT INTO t_subsidy_policy (policy_code, policy_name, category, disability_level, calc_type, amount, threshold_amount, deduct_amount, pay_target, min_stay_days, effective_date, enabled, remark) VALUES
('WU_BAO_LIVING', '五保生活费', 'WU_BAO', NULL, 'FIXED_MONTHLY', 800.00, NULL, NULL, 'ORG', NULL, '2024-01-01', 1, '国家兜底，每月800元，可能随政策上涨'),
('WU_BAO_CARE', '五保生活照料补助', 'WU_BAO', NULL, 'FIXED_MONTHLY', 200.00, NULL, NULL, 'ORG', NULL, '2024-01-01', 1, '财政补助'),
('SOCIAL_OPERATION', '社会化运营补助', 'SOCIAL', NULL, 'FIXED_MONTHLY', 100.00, NULL, NULL, 'ORG', NULL, '2024-01-01', 1, '社会化老人运营补助'),
('LOW_BAO_OPERATION', '低保运营补助', 'LOW_BAO', NULL, 'FIXED_MONTHLY', 100.00, NULL, NULL, 'ORG', NULL, '2024-01-01', 1, '低保对象运营补助'),
('LOW_BAO_SUBSIDY', '低保补助(个人)', 'LOW_BAO', 'MODERATE', 'FIXED_MONTHLY', 300.00, NULL, NULL, 'PERSONAL', 30, '2024-01-01', 1, '入住满30天+中度失能以上，打到个人账户'),
('LONG_CARE_MODERATE', '长护险-中度失能', 'ALL', 'MODERATE', 'DAILY_RATE', 30.00, NULL, NULL, 'ORG', NULL, '2024-01-01', 1, '中度失能30元/天'),
('LONG_CARE_SEVERE', '长护险-重度失能', 'ALL', 'SEVERE', 'DAILY_RATE', 50.00, NULL, NULL, 'ORG', NULL, '2024-01-01', 1, '重度失能50元/天'),
('COUPON_DEDUCT', '消费券抵扣', 'ALL', 'MODERATE', 'THRESHOLD_DEDUCT', 0.00, 2000.00, 800.00, 'ORG', NULL, '2024-01-01', 1, '累计入住金额满2000可抵扣800');
