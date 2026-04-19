-- ========================================
-- 养老企业管理系统 种子数据 (MySQL)
-- 使用 INSERT ... ON DUPLICATE KEY UPDATE 保证幂等性
-- ========================================

-- ========================================
-- 超级管理员用户
-- 密码: Admin123! (BCrypt加密)
-- ========================================
INSERT INTO t_user (id, username, password, real_name, status)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 1)
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
  (32, 3, '打卡记录', 1, '/staff/attendance', 'pages/staff/Attendance', NULL, 2, 'staff:attendance'),
  (33, 3, '排班管理', 1, '/staff/schedule', 'pages/staff/Schedule', NULL, 3, 'staff:schedule')
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
  (41, 4, '缴费管理', 1, '/finance/payment', 'pages/finance/Payment', NULL, 1, 'finance:payment'),
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
-- 上门服务专员角色菜单权限
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
(9, 71),
(9, 72),
(9, 73)
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

-- ========================================
-- 会计科目初始数据
-- ========================================
INSERT INTO t_accounting_subject (code, name, subject_type, direction, enabled, sort_order) VALUES
('1001', '库存现金', 'ASSET', 'DEBIT', 1, 1),
('1002', '银行存款', 'ASSET', 'DEBIT', 1, 2),
('1009', '其他货币资金', 'ASSET', 'DEBIT', 1, 3),
('1122', '应收账款', 'ASSET', 'DEBIT', 1, 4),
('1221', '其他应收款', 'ASSET', 'DEBIT', 1, 5),
('1401', '材料物资', 'ASSET', 'DEBIT', 1, 6),
('1501', '固定资产', 'ASSET', 'DEBIT', 1, 7),
('1502', '累计折旧', 'ASSET', 'CREDIT', 1, 8),
('2001', '应付账款', 'LIABILITY', 'CREDIT', 1, 9),
('2211', '应付职工薪酬', 'LIABILITY', 'CREDIT', 1, 10),
('2221', '应交税费', 'LIABILITY', 'CREDIT', 1, 11),
('2241', '其他应付款', 'LIABILITY', 'CREDIT', 1, 12),
('2501', '预收账款', 'LIABILITY', 'CREDIT', 1, 13),
('5001', '床位费收入', 'INCOME', 'CREDIT', 1, 14),
('5002', '护理费收入', 'INCOME', 'CREDIT', 1, 15),
('5003', '伙食费收入', 'INCOME', 'CREDIT', 1, 16),
('5004', '医疗服务收入', 'INCOME', 'CREDIT', 1, 17),
('5005', '政府补贴收入', 'INCOME', 'CREDIT', 1, 18),
('5006', '居家服务收入', 'INCOME', 'CREDIT', 1, 19),
('5099', '其他收入', 'INCOME', 'CREDIT', 1, 20),
('6001', '职工薪酬', 'EXPENSE', 'DEBIT', 1, 21),
('6002', '伙食成本', 'EXPENSE', 'DEBIT', 1, 22),
('6003', '医药成本', 'EXPENSE', 'DEBIT', 1, 23),
('6004', '水电费', 'EXPENSE', 'DEBIT', 1, 24),
('6005', '物资消耗', 'EXPENSE', 'DEBIT', 1, 25),
('6006', '设备维护费', 'EXPENSE', 'DEBIT', 1, 26),
('6007', '折旧费用', 'EXPENSE', 'DEBIT', 1, 27),
('6099', '其他费用', 'EXPENSE', 'DEBIT', 1, 28);

-- ========== 服务项目(国标 GB/T 43153-2023) ==========
INSERT INTO t_service_item (name, category, price, unit, description, status) VALUES
-- 生活照料服务(6项)
('助餐服务', '生活照料', 30.00, '次', '协助订餐、上门送餐、上门烹任等', 1),
('助浴服务', '生活照料', 50.00, '次', '上门助浴、协助前往助浴点进行身体清洁', 1),
('助洁服务', '生活照料', 40.00, '次', '洗漱、理发、剃须、身体助洁、居家清洁、衣物洗涤、物品整理等', 1),
('助行服务', '生活照料', 35.00, '次', '协助行走、陪同外出等', 1),
('助医服务', '生活照料', 60.00, '次', '陪同就医、治疗陪伴等', 1),
('助急服务', '生活照料', 0.00, '次', '紧急呼叫受理、紧急转介等', 1),
-- 基础照护服务(5项)
('生活照护', '基础照护', 50.00, '小时', '协助穿脱衣、饮食照护、睡眠照护等', 1),
('排泄护理', '基础照护', 50.00, '次', '排尿护理、排便护理、排气护理等', 1),
('护理协助', '基础照护', 45.00, '次', '保暖和物理降温、翻身拍背、褥疮预防等', 1),
('用药照护', '基础照护', 30.00, '次', '用药提醒、用药指导及不良反应观察等', 1),
('康复护理', '基础照护', 80.00, '次', '康复评估、计划制定、康复训练指导、辅助器具使用等', 1),
-- 健康管理服务(4项)
('健康信息采集', '健康管理', 30.00, '次', '体检信息、既往疾病史等健康信息采集，建立健康档案', 1),
('健康监测', '健康管理', 25.00, '次', '体温、体重、血压、呼吸、心率、血糖等生理指数监测', 1),
('健康咨询', '健康管理', 40.00, '次', '防跌倒、疾病预防、膳食营养、康复保健等指导', 1),
('健康干预', '健康管理', 50.00, '次', '制定服务方案，生活起居、慢病调理等干预服务', 1),
-- 探访关爱服务(2项)
('上门探访', '探访关爱', 20.00, '次', '了解健康状况、精神状况、安全情况、卫生状况、居住环境等', 1),
('应急处置', '探访关爱', 0.00, '次', '接受与协助老年人的电话呼叫、紧急求助等', 1),
-- 精神慰藉服务(3项)
('陪伴支持', '精神慰藉', 30.00, '小时', '定期协助老年人外出活动或参加集体活动等', 1),
('情绪疏导', '精神慰藉', 40.00, '次', '与老年人进行谠心交流，耐心倾听老年人的诉说', 1),
('心理慰藉', '精神慰藉', 60.00, '次', '心理健康教育、心理干预手段调整老年人心理状态', 1),
-- 委托代办服务(3项)
('代购服务', '委托代办', 15.00, '次', '购买日常生活用品、订车票、预约车辆等', 1),
('代办服务', '委托代办', 20.00, '次', '取送信函、文件和物品，申请法律援助、救助服务等', 1),
('代缴服务', '委托代办', 10.00, '次', '缴纳水、电、气、通讯费等日常费用', 1),
-- 适老化改造服务(3项)
('环境评估', '适老化改造', 100.00, '次', '评估老年人家庭生活环境和改造需求，确定改造方案', 1),
('基础改造', '适老化改造', 500.00, '项', '防滑、防摔、防走失等物理环境改造及设备与用品配置', 1),
('专项改造', '适老化改造', 1000.00, '项', '健康监测、安防报警、远程控制等智能家居产品配置安装', 1),
-- 其他个性化服务(2项)
('个性化照护', '其他个性化', 0.00, '次', '根据老年人特殊需求定制的个性化服务', 1),
('临时特殊服务', '其他个性化', 0.00, '次', '其他临时性、特殊性的养老服务需求', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  category = VALUES(category),
  price = VALUES(price),
  unit = VALUES(unit),
  description = VALUES(description),
  status = VALUES(status);
