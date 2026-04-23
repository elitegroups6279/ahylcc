-- ========================================
-- 养老企业管理系统 - 角色权限修复脚本
-- 修复内容：
-- 0. 补充缺失的页面级菜单(type=1)
-- 1. 添加按钮级菜单(type=2)
-- 2. 为所有非超管角色添加菜单关联
-- 3. 确保 SUPER_ADMIN 拥有所有菜单
-- ========================================

-- ========================================
-- 0. 补充缺失的页面级菜单 (type=1)
-- 旧版部署可能没有这些菜单
-- ========================================
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, visible, status) VALUES
  (34, 3, '员工管理', 1, '/staff/employees', 'pages/staff/Employees', 'staff:employee', NULL, 4, 1, 1),
  (44, 4, '月度账单', 1, '/finance/bills', 'pages/finance/Bills', 'finance:bill', NULL, 4, 1, 1),
  (45, 4, '补贴政策', 1, '/finance/subsidy', 'pages/finance/Subsidy', 'system:config', NULL, 5, 1, 1),
  (73, 7, '服务评估', 1, '/home-service/assessment', 'pages/home-service/Assessment', 'home-service:assessment', NULL, 3, 1, 1);

-- ========================================
-- 1. 添加按钮级菜单 (type=2)
-- 这些菜单对应控制器中的具体操作，权限字符串与页面级菜单一致
-- ========================================

-- 入住管理 - 在住老人列表 (parent_id=21)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (201, 21, '查看详情', 2, NULL, NULL, NULL, 1, 'elderly:list'),
  (202, 21, '编辑档案', 2, NULL, NULL, NULL, 2, 'elderly:list'),
  (203, 21, '退住', 2, NULL, NULL, NULL, 3, 'elderly:list'),
  (204, 21, '撤销退住', 2, NULL, NULL, NULL, 4, 'elderly:list'),
  (205, 21, '转床', 2, NULL, NULL, NULL, 5, 'elderly:list'),
  (206, 21, '导入模板下载', 2, NULL, NULL, NULL, 6, 'elderly:list'),
  (207, 21, '批量导入', 2, NULL, NULL, NULL, 7, 'elderly:list'),
  (208, 21, '请假', 2, NULL, NULL, NULL, 8, 'elderly:list'),
  (209, 21, '销假', 2, NULL, NULL, NULL, 9, 'elderly:list');

-- 入住管理 - 新增入住 (parent_id=22)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (210, 22, '提交入住', 2, NULL, NULL, NULL, 1, 'elderly:add');

-- 人事管理 - 护工管理 (parent_id=31)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (301, 31, '新增护工', 2, NULL, NULL, NULL, 1, 'staff:list'),
  (302, 31, '编辑护工', 2, NULL, NULL, NULL, 2, 'staff:list'),
  (303, 31, '删除护工', 2, NULL, NULL, NULL, 3, 'staff:list'),
  (304, 31, '离职处理', 2, NULL, NULL, NULL, 4, 'staff:list');

-- 人事管理 - 员工管理 (parent_id=34)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (341, 34, '新增员工', 2, NULL, NULL, NULL, 1, 'staff:employee'),
  (342, 34, '编辑员工', 2, NULL, NULL, NULL, 2, 'staff:employee'),
  (343, 34, '删除员工', 2, NULL, NULL, NULL, 3, 'staff:employee');

-- 人事管理 - 打卡记录 (parent_id=32)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (321, 32, '补录打卡', 2, NULL, NULL, NULL, 1, 'staff:attendance');

-- 人事管理 - 排班管理 (parent_id=33)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (331, 33, '新增排班', 2, NULL, NULL, NULL, 1, 'staff:schedule'),
  (332, 33, '编辑排班', 2, NULL, NULL, NULL, 2, 'staff:schedule'),
  (333, 33, '删除排班', 2, NULL, NULL, NULL, 3, 'staff:schedule'),
  (334, 33, '批量排班', 2, NULL, NULL, NULL, 4, 'staff:schedule');

-- 财务管理 - 收支管理 (parent_id=41)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (411, 41, '删除支出', 2, NULL, NULL, NULL, 1, 'finance:cashflow'),
  (412, 41, '删除收入', 2, NULL, NULL, NULL, 2, 'finance:cashflow');

-- 财务管理 - 凭证管理 (parent_id=42)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (421, 42, '新增凭证', 2, NULL, NULL, NULL, 1, 'finance:voucher'),
  (422, 42, '编辑凭证', 2, NULL, NULL, NULL, 2, 'finance:voucher'),
  (423, 42, '删除凭证', 2, NULL, NULL, NULL, 3, 'finance:voucher'),
  (424, 42, '提交审核', 2, NULL, NULL, NULL, 4, 'finance:voucher'),
  (425, 42, '审核通过', 2, NULL, NULL, NULL, 5, 'finance:voucher'),
  (426, 42, '审核驳回', 2, NULL, NULL, NULL, 6, 'finance:voucher');

-- 财务管理 - 报账管理 (parent_id=43)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (431, 43, '创建报账单', 2, NULL, NULL, NULL, 1, 'finance:reimbursement'),
  (432, 43, '审批通过', 2, NULL, NULL, NULL, 2, 'finance:reimbursement'),
  (433, 43, '审批驳回', 2, NULL, NULL, NULL, 3, 'finance:reimbursement'),
  (434, 43, '标记已支付', 2, NULL, NULL, NULL, 4, 'finance:reimbursement'),
  (435, 43, '删除报账单', 2, NULL, NULL, NULL, 5, 'finance:reimbursement');

-- 财务管理 - 月度账单 (parent_id=44)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (441, 44, '生成月账单', 2, NULL, NULL, NULL, 1, 'finance:bill'),
  (442, 44, '确认月结算', 2, NULL, NULL, NULL, 2, 'finance:bill');

-- 仓库管理 - 物资档案 (parent_id=50)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (501, 50, '新增物资', 2, NULL, NULL, NULL, 1, 'warehouse:materials'),
  (502, 50, '编辑物资', 2, NULL, NULL, NULL, 2, 'warehouse:materials'),
  (503, 50, '删除物资', 2, NULL, NULL, NULL, 3, 'warehouse:materials');

-- 仓库管理 - 入库管理 (parent_id=51)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (511, 51, '入库登记', 2, NULL, NULL, NULL, 1, 'warehouse:in');

-- 仓库管理 - 出库管理 (parent_id=52)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (521, 52, '出库登记', 2, NULL, NULL, NULL, 1, 'warehouse:out');

-- 仓库管理 - 库存看板 (parent_id=53)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (531, 53, '查看库存', 2, NULL, NULL, NULL, 1, 'warehouse:stock');

-- 仓库管理 - 盘点管理 (parent_id=54)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (541, 54, '盘点登记', 2, NULL, NULL, NULL, 1, 'warehouse:check');

-- 药物管理 - 药品档案 (parent_id=61)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (611, 61, '新增药品', 2, NULL, NULL, NULL, 1, 'pharmacy:drugs'),
  (612, 61, '编辑药品', 2, NULL, NULL, NULL, 2, 'pharmacy:drugs'),
  (613, 61, '删除药品', 2, NULL, NULL, NULL, 3, 'pharmacy:drugs');

-- 药物管理 - 发药管理 (parent_id=62)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (621, 62, '创建发药单', 2, NULL, NULL, NULL, 1, 'pharmacy:dispense'),
  (622, 62, '确认发药', 2, NULL, NULL, NULL, 2, 'pharmacy:dispense'),
  (623, 62, '药品批次入库', 2, NULL, NULL, NULL, 3, 'pharmacy:dispense'),
  (624, 62, '效期预警查看', 2, NULL, NULL, NULL, 4, 'pharmacy:dispense');

-- 上门服务 - 预约管理 (parent_id=71)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (711, 71, '创建预约单', 2, NULL, NULL, NULL, 1, 'home-service:orders'),
  (712, 71, '更新预约单', 2, NULL, NULL, NULL, 2, 'home-service:orders');

-- 上门服务 - 服务记录 (parent_id=72)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (721, 72, '提交服务记录', 2, NULL, NULL, NULL, 1, 'home-service:records');

-- 上门服务 - 服务评估 (parent_id=73)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (731, 73, '创建评估', 2, NULL, NULL, NULL, 1, 'home-service:assessment'),
  (732, 73, '编辑评估', 2, NULL, NULL, NULL, 2, 'home-service:assessment'),
  (733, 73, '删除评估', 2, NULL, NULL, NULL, 3, 'home-service:assessment'),
  (734, 73, '提交评估', 2, NULL, NULL, NULL, 4, 'home-service:assessment'),
  (735, 73, '确认评估', 2, NULL, NULL, NULL, 5, 'home-service:assessment');

-- 报表管理 - 报表导出 (parent_id=91)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (911, 91, '导出费用汇总', 2, NULL, NULL, NULL, 1, 'report:export'),
  (912, 91, '导出人员名册', 2, NULL, NULL, NULL, 2, 'report:export'),
  (913, 91, '导出考勤报表', 2, NULL, NULL, NULL, 3, 'report:export'),
  (914, 91, '导出物资消耗', 2, NULL, NULL, NULL, 4, 'report:export');

-- 系统管理 - 账户管理 (parent_id=81)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (811, 81, '创建用户', 2, NULL, NULL, NULL, 1, 'system:accounts'),
  (812, 81, '编辑用户', 2, NULL, NULL, NULL, 2, 'system:accounts'),
  (813, 81, '删除用户', 2, NULL, NULL, NULL, 3, 'system:accounts'),
  (814, 81, '重置密码', 2, NULL, NULL, NULL, 4, 'system:accounts'),
  (815, 81, '启用/停用', 2, NULL, NULL, NULL, 5, 'system:accounts');

-- 系统管理 - 角色管理 (parent_id=82)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (821, 82, '创建角色', 2, NULL, NULL, NULL, 1, 'system:roles'),
  (822, 82, '编辑角色', 2, NULL, NULL, NULL, 2, 'system:roles'),
  (823, 82, '删除角色', 2, NULL, NULL, NULL, 3, 'system:roles');

-- 系统管理 - 菜单管理 (parent_id=83)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (831, 83, '创建菜单', 2, NULL, NULL, NULL, 1, 'system:menus'),
  (832, 83, '编辑菜单', 2, NULL, NULL, NULL, 2, 'system:menus'),
  (833, 83, '删除菜单', 2, NULL, NULL, NULL, 3, 'system:menus');

-- 系统管理 - 系统配置 (parent_id=84)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (841, 84, '更新配置', 2, NULL, NULL, NULL, 1, 'system:config');

-- 系统管理 - 操作日志 (parent_id=85)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (851, 85, '查看日志', 2, NULL, NULL, NULL, 1, 'system:logs');

-- 补贴政策 (parent_id=45)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (451, 45, '新增补贴策略', 2, NULL, NULL, NULL, 1, 'system:config'),
  (452, 45, '编辑补贴策略', 2, NULL, NULL, NULL, 2, 'system:config'),
  (453, 45, '删除补贴策略', 2, NULL, NULL, NULL, 3, 'system:config'),
  (454, 45, '启用/禁用', 2, NULL, NULL, NULL, 4, 'system:config');


-- ========================================
-- 2. 清除旧的角色-菜单关联（保留 SUPER_ADMIN）
-- ========================================
DELETE FROM t_role_menu WHERE role_id != 1;

-- ========================================
-- 3. SUPER_ADMIN 关联所有菜单（包括新增的按钮菜单）
-- ========================================
INSERT IGNORE INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu;

-- ========================================
-- 4. 机构负责人 ORG_MANAGER (role_id=2)
-- 拥有几乎所有权限，包括系统管理
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(2, 1),
-- 入住管理
(2, 2), (2, 21), (2, 22), (2, 23), (2, 24),
(2, 201), (2, 202), (2, 203), (2, 204), (2, 205), (2, 206), (2, 207), (2, 208), (2, 209), (2, 210),
-- 人事管理
(2, 3), (2, 31), (2, 34), (2, 32), (2, 33),
(2, 301), (2, 302), (2, 303), (2, 304), (2, 341), (2, 342), (2, 343), (2, 321), (2, 331), (2, 332), (2, 333), (2, 334),
-- 财务管理
(2, 4), (2, 41), (2, 42), (2, 43), (2, 44), (2, 45),
(2, 411), (2, 412), (2, 421), (2, 422), (2, 423), (2, 424), (2, 425), (2, 426),
(2, 431), (2, 432), (2, 433), (2, 434), (2, 435), (2, 441), (2, 442), (2, 451), (2, 452), (2, 453), (2, 454),
-- 仓库管理
(2, 5), (2, 50), (2, 51), (2, 52), (2, 53), (2, 54),
(2, 501), (2, 502), (2, 503), (2, 511), (2, 521), (2, 531), (2, 541),
-- 药物管理
(2, 6), (2, 61), (2, 62),
(2, 611), (2, 612), (2, 613), (2, 621), (2, 622), (2, 623), (2, 624),
-- 上门服务
(2, 7), (2, 71), (2, 72), (2, 73),
(2, 711), (2, 712), (2, 721), (2, 731), (2, 732), (2, 733), (2, 734), (2, 735),
-- 系统管理
(2, 8), (2, 81), (2, 82), (2, 83), (2, 84), (2, 85),
(2, 811), (2, 812), (2, 813), (2, 814), (2, 815), (2, 821), (2, 822), (2, 823), (2, 831), (2, 832), (2, 833), (2, 841), (2, 851),
-- 报表管理
(2, 9), (2, 91), (2, 911), (2, 912), (2, 913), (2, 914);

-- ========================================
-- 5. 财务人员 FINANCE (role_id=3)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(3, 1),
-- 入住管理（仅查看）
(3, 2), (3, 21), (3, 201), (3, 202), (3, 206), (3, 207), (3, 208), (3, 209),
-- 财务管理（全部）
(3, 4), (3, 41), (3, 42), (3, 43), (3, 44), (3, 45),
(3, 411), (3, 412), (3, 421), (3, 422), (3, 423), (3, 424), (3, 425), (3, 426),
(3, 431), (3, 432), (3, 433), (3, 434), (3, 435), (3, 441), (3, 442), (3, 451), (3, 452), (3, 453), (3, 454),
-- 报表管理
(3, 9), (3, 91), (3, 911), (3, 912), (3, 913), (3, 914);

-- ========================================
-- 6. 人事专员 HR (role_id=4)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(4, 1),
-- 入住管理（仅查看）
(4, 2), (4, 21), (4, 201), (4, 202), (4, 206), (4, 207), (4, 208), (4, 209),
-- 人事管理（全部）
(4, 3), (4, 31), (4, 34), (4, 32), (4, 33),
(4, 301), (4, 302), (4, 303), (4, 304), (4, 341), (4, 342), (4, 343), (4, 321), (4, 331), (4, 332), (4, 333), (4, 334),
-- 报表管理
(4, 9), (4, 91), (4, 911), (4, 912), (4, 913), (4, 914);

-- ========================================
-- 7. 护理主管 NURSING_SUPERVISOR (role_id=5)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(5, 1),
-- 入住管理（全部）
(5, 2), (5, 21), (5, 22), (5, 23), (5, 24),
(5, 201), (5, 202), (5, 203), (5, 204), (5, 205), (5, 206), (5, 207), (5, 208), (5, 209), (5, 210),
-- 人事管理（查看）
(5, 3), (5, 31), (5, 32), (5, 301), (5, 302), (5, 303), (5, 304), (5, 321),
-- 报表管理
(5, 9), (5, 91), (5, 911), (5, 912), (5, 913), (5, 914);

-- ========================================
-- 8. 护工 CAREGIVER (role_id=6)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(6, 1),
-- 入住管理（仅查看）
(6, 2), (6, 21), (6, 201), (6, 202), (6, 206), (6, 207), (6, 208), (6, 209),
-- 人事管理（打卡记录）
(6, 3), (6, 32), (6, 321);

-- ========================================
-- 9. 仓库员 WAREHOUSE (role_id=7)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(7, 1),
-- 仓库管理（全部）
(7, 5), (7, 50), (7, 51), (7, 52), (7, 53), (7, 54),
(7, 501), (7, 502), (7, 503), (7, 511), (7, 521), (7, 531), (7, 541),
-- 报表管理
(7, 9), (7, 91), (7, 911), (7, 912), (7, 913), (7, 914);

-- ========================================
-- 10. 药剂员 PHARMACIST (role_id=8)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(8, 1),
-- 药物管理（全部）
(8, 6), (8, 61), (8, 62),
(8, 611), (8, 612), (8, 613), (8, 621), (8, 622), (8, 623), (8, 624),
-- 报表管理
(8, 9), (8, 91), (8, 911), (8, 912), (8, 913), (8, 914);

-- ========================================
-- 11. 上门服务专员 HOME_SERVICE (role_id=9)
-- ========================================
INSERT INTO t_role_menu (role_id, menu_id) VALUES
-- 首页
(9, 1),
-- 入住管理（仅查看）
(9, 2), (9, 21), (9, 201), (9, 202), (9, 206), (9, 207), (9, 208), (9, 209),
-- 上门服务（全部）
(9, 7), (9, 71), (9, 72), (9, 73),
(9, 711), (9, 712), (9, 721), (9, 731), (9, 732), (9, 733), (9, 734), (9, 735);
