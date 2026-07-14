-- ========================================
-- 养老企业管理系统 数据库表结构 (H2 MySQL兼容模式)
-- ========================================

-- ========================================
-- 权限相关表
-- ========================================

-- t_user: 系统用户
CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL,
  real_name VARCHAR(50),
  phone VARCHAR(50),
  email VARCHAR(100),
  avatar VARCHAR(500),
  status TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  last_login_time DATETIME,
  last_login_ip VARCHAR(50),
  org_id BIGINT COMMENT '所属机构ID(NULL=超级管理员)',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_role: 角色
CREATE TABLE IF NOT EXISTS t_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL UNIQUE,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_menu: 菜单
CREATE TABLE IF NOT EXISTS t_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0,
  menu_name VARCHAR(50) NOT NULL,
  menu_type TINYINT COMMENT '0目录 1菜单 2按钮',
  path VARCHAR(200),
  component VARCHAR(200),
  permission VARCHAR(100),
  icon VARCHAR(100),
  sort_order INT DEFAULT 0,
  visible TINYINT DEFAULT 1,
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_user_role: 用户-角色关联
CREATE TABLE IF NOT EXISTS t_user_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE(user_id, role_id)
);

-- t_role_menu: 角色-菜单关联
CREATE TABLE IF NOT EXISTS t_role_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  UNIQUE(role_id, menu_id)
);

-- t_organization: 机构表
CREATE TABLE IF NOT EXISTS t_organization (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  org_code VARCHAR(50) NOT NULL UNIQUE COMMENT '机构编码',
  org_name VARCHAR(100) NOT NULL COMMENT '机构名称',
  address VARCHAR(300) COMMENT '地址',
  phone VARCHAR(50) COMMENT '联系电话',
  contact_person VARCHAR(50) COMMENT '联系人',
  status TINYINT DEFAULT 1 COMMENT '1=启用 0=停用',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '机构表';

-- ========================================
-- 业务表 - 入住管理
-- ========================================

-- t_bed: 床位
CREATE TABLE IF NOT EXISTS t_bed (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  building VARCHAR(50) COMMENT '楼栋',
  floor VARCHAR(20) COMMENT '楼层',
  room_number VARCHAR(20) NOT NULL COMMENT '房间号',
  bed_number VARCHAR(20) NOT NULL COMMENT '床位号',
  status TINYINT DEFAULT 0 COMMENT '0空闲 1占用 2维修',
  description VARCHAR(200),
  org_id BIGINT COMMENT '所属机构ID',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_elderly: 老人
CREATE TABLE IF NOT EXISTS t_elderly (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  unique_no VARCHAR(20) NOT NULL UNIQUE COMMENT '唯一编号 YYYYMM+4位序号',
  name VARCHAR(50) NOT NULL,
  id_card VARCHAR(200) NOT NULL COMMENT '身份证号(加密存储)',
  gender TINYINT COMMENT '0女 1男',
  birth_date DATE,
  age INT,
  region VARCHAR(100) COMMENT '籍贯',
  ethnicity VARCHAR(50) COMMENT '民族',
  photo_url VARCHAR(500) COMMENT '照片URL',
  admission_date DATE NOT NULL COMMENT '入住日期',
  bed_id BIGINT COMMENT '床位ID',
  category VARCHAR(20) NOT NULL COMMENT 'SOCIAL/LOW_BAO/WU_BAO',
  care_level VARCHAR(30) COMMENT '护理等级',
  disability_level VARCHAR(20) DEFAULT 'INTACT' COMMENT '失能等级: INTACT能力完好/MILD轻度/MODERATE中度/SEVERE重度/TOTAL完全失能; legacy: SELF_CARE',
  enable_long_care TINYINT DEFAULT 0 COMMENT '是否享受长护险',
  enable_coupon TINYINT DEFAULT 0 COMMENT '是否使用消费券',
  nursing_needs VARCHAR(1000) COMMENT '护理需求JSON',
  medical_history VARCHAR(1000) COMMENT '既往疾病JSON',
  hospital VARCHAR(200) COMMENT '主治医疗机构',
  doctor VARCHAR(100) COMMENT '主治医生',
  regular_medication VARCHAR(500) COMMENT '常规用药',
  contract_monthly_fee DECIMAL(10,2) COMMENT '合同月费',
  deposit DECIMAL(10,2) COMMENT '押金',
  contract_start_date DATE COMMENT '合同起始日期',
  contract_months INT COMMENT '合同有效期月数',
  contract_attachment_url VARCHAR(500) COMMENT '合同附件URL',
  payment_method VARCHAR(20) COMMENT 'MONTHLY/QUARTERLY/YEARLY/ONCE',
  bank_account VARCHAR(100) COMMENT '银行账户',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE在住/DISCHARGED退住/ON_LEAVE请假中',
  discharge_date DATE,
  discharge_reason VARCHAR(200),
  org_id BIGINT COMMENT '所属机构ID',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_elderly_contact: 家属联系人
CREATE TABLE IF NOT EXISTS t_elderly_contact (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  relationship VARCHAR(20) COMMENT '子女/配偶/其他',
  phone VARCHAR(50) NOT NULL,
  is_emergency TINYINT DEFAULT 0 COMMENT '是否紧急联系人',
  sort_order INT DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 护工/人事
-- ========================================

-- t_staff: 护工
CREATE TABLE IF NOT EXISTS t_staff (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  id_card VARCHAR(200) COMMENT '身份证(加密)',
  gender TINYINT,
  birth_date DATE,
  age INT,
  education VARCHAR(50) COMMENT '学历',
  phone VARCHAR(50),
  emergency_contact VARCHAR(100) COMMENT '紧急联系人',
  emergency_phone VARCHAR(50),
  hire_date DATE COMMENT '入职日期',
  resign_date DATE COMMENT '离职日期',
  resign_reason VARCHAR(200),
  job_type VARCHAR(20) DEFAULT 'FULL' COMMENT 'FULL全职/PART兼职',
  qualification_urls VARCHAR(1000) COMMENT '资质证书URL JSON',
  base_salary DECIMAL(10,2) COMMENT '基本工资',
  probation_status VARCHAR(20) DEFAULT 'FORMAL' COMMENT 'INTERN实习/FORMAL正式',
  probation_months INT COMMENT '实习期月数',
  probation_end_date DATE COMMENT '实习到期日期',
  has_caregiver_cert TINYINT DEFAULT 0 COMMENT '是否有护工证：0=无，1=有',
  has_health_cert TINYINT DEFAULT 0 COMMENT '是否有健康证：0=无，1=有',
  position_type VARCHAR(20) DEFAULT 'CAREGIVER',
  org_id BIGINT COMMENT '所属机构ID',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE在职/RESIGNED离职',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_staff_assignment: 护工-老人关联
CREATE TABLE IF NOT EXISTS t_staff_assignment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  assign_type VARCHAR(20) DEFAULT 'PRIMARY' COMMENT 'PRIMARY主责/SECONDARY备用',
  start_time DATETIME NOT NULL,
  end_time DATETIME,
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_attendance: 打卡记录
CREATE TABLE IF NOT EXISTS t_attendance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  clock_in_time DATETIME,
  clock_out_time DATETIME,
  status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL正常/LATE迟到/EARLY早退/ABSENT缺勤',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_schedule: 排班
CREATE TABLE IF NOT EXISTS t_schedule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  shift_type VARCHAR(20) NOT NULL COMMENT 'MORNING早班/AFTERNOON中班/NIGHT夜班',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 财务
-- ========================================

-- t_fee_account: 费用账户
CREATE TABLE IF NOT EXISTS t_fee_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL UNIQUE,
  balance DECIMAL(12,2) DEFAULT 0 COMMENT '账户余额',
  total_charged DECIMAL(12,2) DEFAULT 0 COMMENT '累计缴费',
  total_consumed DECIMAL(12,2) DEFAULT 0 COMMENT '累计消费',
  carry_over DECIMAL(10,2) DEFAULT 0 COMMENT '顺延金额',
  warning_status TINYINT DEFAULT 0 COMMENT '0正常 1预警',
  org_id BIGINT COMMENT '所属机构ID',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_fee_bill: 月账单
CREATE TABLE IF NOT EXISTS t_fee_bill (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  bill_month VARCHAR(7) NOT NULL COMMENT '账单月份 YYYY-MM',
  amount_due DECIMAL(10,2) NOT NULL COMMENT '应收金额',
  amount_paid DECIMAL(10,2) DEFAULT 0 COMMENT '实收金额',
  carry_over_in DECIMAL(10,2) DEFAULT 0 COMMENT '上月顺延冲抵',
  carry_over_out DECIMAL(10,2) DEFAULT 0 COMMENT '本月顺延至下月',
  stay_days INT COMMENT '实住天数',
  leave_days INT DEFAULT 0 COMMENT '请假天数',
  billing_rule VARCHAR(10) COMMENT 'A短期/B正常',
  base_fee DECIMAL(12,2) DEFAULT 0 COMMENT '基础费用',
  long_care_amount DECIMAL(12,2) DEFAULT 0 COMMENT '长护险补贴',
  coupon_deduct DECIMAL(12,2) DEFAULT 0 COMMENT '消费券抵扣',
  subsidy_amount DECIMAL(12,2) DEFAULT 0 COMMENT '财政补助合计',
  personal_subsidy DECIMAL(12,2) DEFAULT 0 COMMENT '个人账户补助(低保)',
  family_payable DECIMAL(12,2) DEFAULT 0 COMMENT '家属应缴',
  gov_payable DECIMAL(12,2) DEFAULT 0 COMMENT '政府应拨',
  subsidy_detail TEXT COMMENT '补贴明细JSON',
  org_id BIGINT COMMENT '所属机构ID',
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/CONFIRMED已确认/SETTLED已结清',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_payment_record: 缴费记录
CREATE TABLE IF NOT EXISTS t_payment_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT COMMENT '老人ID(养老费用时必填)',
  amount DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(30) COMMENT 'CASH/TRANSFER/POS',
  source_type VARCHAR(30) COMMENT 'LONG_CARE长护险/COUPON消费券/OTHER其他',
  income_type VARCHAR(30) COMMENT '收入类型：ELDERLY_FEE/SUBSIDY/DONATION/RENTAL/OTHER',
  description VARCHAR(500) COMMENT '收入说明',
  voucher_url VARCHAR(500) COMMENT '凭证图片URL',
  receipt_no VARCHAR(50),
  operator_id BIGINT COMMENT '操作员ID',
  payment_date DATE COMMENT '缴费时间',
  validity_start_date DATE COMMENT '费用有效期开始',
  validity_end_date DATE COMMENT '费用有效期结束',
  remark VARCHAR(200),
  org_id BIGINT COMMENT '所属机构ID',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_voucher: 凭证
CREATE TABLE IF NOT EXISTS t_voucher (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  voucher_no VARCHAR(30) NOT NULL UNIQUE COMMENT '凭证编号 年月+序号',
  voucher_type VARCHAR(20) NOT NULL COMMENT 'INCOME收入/EXPENSE支出',
  category VARCHAR(50) COMMENT '类别: 缴费/采购/薪资/水电',
  amount DECIMAL(10,2) NOT NULL,
  related_id BIGINT COMMENT '关联业务ID',
  attachment_url VARCHAR(500),
  description VARCHAR(500),
  operator_id BIGINT,
  voucher_date DATE NOT NULL,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_reimbursement: 报账
CREATE TABLE IF NOT EXISTS t_reimbursement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  amount DECIMAL(10,2) NOT NULL,
  reason VARCHAR(500) NOT NULL COMMENT '报账事由',
  attachment_urls VARCHAR(1000) COMMENT '附件URL JSON',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVING/APPROVED/REJECTED/PAID',
  approver_id BIGINT COMMENT '审批人ID',
  approve_time DATETIME,
  reviewer_id BIGINT COMMENT '财务审核人ID',
  review_time DATETIME,
  reject_reason VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 仓库
-- ========================================

-- t_material: 物资
CREATE TABLE IF NOT EXISTS t_material (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) COMMENT '生活用品/护理耗材/医疗设备/清洁消毒/办公用品',
  specification VARCHAR(100) COMMENT '规格型号',
  unit VARCHAR(20) COMMENT '单位',
  warning_threshold INT DEFAULT 10 COMMENT '库存预警阈值',
  description VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_stock: 库存（双轨制: SOCIAL社会化/CENTRALIZED集中供养）
CREATE TABLE IF NOT EXISTS t_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL,
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT 'SOCIAL/CENTRALIZED',
  quantity INT DEFAULT 0,
  total_value DECIMAL(12,2) DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_material_category ON t_stock(material_id, supply_category);

-- t_inventory_in: 入库记录（双模式: DIRECT直接入库/FROM_PURCHASE从采购单入库）
CREATE TABLE IF NOT EXISTS t_inventory_in (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL,
  supplier VARCHAR(100) COMMENT '供应商',
  purchase_order_no VARCHAR(50) COMMENT '采购单号',
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2),
  total_amount DECIMAL(10,2),
  in_date DATE NOT NULL,
  operator_id BIGINT,
  attachment_url VARCHAR(500),
  remark VARCHAR(200),
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT 'SOCIAL/CENTRALIZED',
  allocation_id BIGINT COMMENT '关联五保拨款批次',
  purchase_receipt_id BIGINT COMMENT '关联验收记录',
  in_mode VARCHAR(20) DEFAULT 'DIRECT' COMMENT 'DIRECT/FROM_PURCHASE',
  expense_record_id BIGINT COMMENT '关联支出记录',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_inventory_out: 出库记录
CREATE TABLE IF NOT EXISTS t_inventory_out (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL,
  department VARCHAR(50) COMMENT '领用部门',
  purpose VARCHAR(200) COMMENT '用途',
  quantity INT NOT NULL,
  operator_id BIGINT,
  out_date DATE NOT NULL,
  status VARCHAR(20) DEFAULT 'APPROVED' COMMENT 'PENDING/APPROVED/REJECTED',
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT 'SOCIAL/CENTRALIZED',
  recipient_staff_id BIGINT COMMENT '领用护工ID',
  recipient_name VARCHAR(50) COMMENT '领用人姓名',
  recipient_sign_url VARCHAR(500) COMMENT '签字图片URL',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 药物
-- ========================================

-- t_drug: 药品档案
CREATE TABLE IF NOT EXISTS t_drug (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '药品名称',
  generic_name VARCHAR(100) COMMENT '通用名',
  specification VARCHAR(100) COMMENT '规格',
  dosage_form VARCHAR(50) COMMENT '剂型',
  manufacturer VARCHAR(200) COMMENT '厂家',
  approval_number VARCHAR(100) COMMENT '批准文号',
  storage_condition VARCHAR(200) COMMENT '存储条件',
  is_prescription TINYINT DEFAULT 0 COMMENT '是否处方药',
  warning_days INT DEFAULT 30 COMMENT '近效期预警天数',
  description VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_drug_batch: 药品批次
CREATE TABLE IF NOT EXISTS t_drug_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  drug_id BIGINT NOT NULL,
  batch_no VARCHAR(50) NOT NULL COMMENT '批号',
  quantity INT NOT NULL COMMENT '数量',
  remaining INT NOT NULL COMMENT '剩余数量',
  expiry_date DATE NOT NULL COMMENT '有效期',
  in_date DATE NOT NULL COMMENT '入库日期',
  supplier VARCHAR(100),
  operator_id BIGINT,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_dispense_order: 发药单
CREATE TABLE IF NOT EXISTS t_dispense_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  order_date DATE NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/DISPENSED/CANCELLED',
  operator_id BIGINT,
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_dispense_record: 发药记录
CREATE TABLE IF NOT EXISTS t_dispense_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  drug_id BIGINT NOT NULL,
  batch_id BIGINT,
  dosage VARCHAR(50) COMMENT '剂量',
  quantity INT NOT NULL,
  dispense_time DATETIME NOT NULL,
  executor_id BIGINT COMMENT '执行护工ID',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 上门服务
-- ========================================

-- t_service_item: 服务项目
CREATE TABLE IF NOT EXISTS t_service_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) COMMENT '服务大类',
  price DECIMAL(10,2) NOT NULL COMMENT '收费标准',
  unit VARCHAR(20) COMMENT '计费单位(次/小时)',
  description VARCHAR(500),
  status TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_home_service_order: 上门服务订单
CREATE TABLE IF NOT EXISTS t_home_service_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT,
  service_item_id BIGINT NOT NULL,
  expected_time DATETIME COMMENT '期望服务时间',
  address VARCHAR(300) COMMENT '服务地址',
  special_note VARCHAR(500) COMMENT '特殊说明',
  assigned_staff_id BIGINT COMMENT '分配服务人员',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_home_service_record: 服务记录
CREATE TABLE IF NOT EXISTS t_home_service_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  actual_start_time DATETIME,
  actual_end_time DATETIME,
  service_content VARCHAR(1000) COMMENT '服务内容',
  signature_url VARCHAR(500) COMMENT '签字图片URL',
  rating INT COMMENT '评分1-5',
  amount DECIMAL(10,2) COMMENT '服务费用',
  payment_status VARCHAR(20) DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/MERGED合并结算',
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 系统表
-- ========================================

-- t_operation_log: 操作日志
CREATE TABLE IF NOT EXISTS t_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  username VARCHAR(50),
  module VARCHAR(50) COMMENT '操作模块',
  operation VARCHAR(100) COMMENT '操作内容',
  method VARCHAR(200) COMMENT '请求方法',
  params TEXT COMMENT '请求参数',
  old_value TEXT COMMENT '修改前值',
  new_value TEXT COMMENT '修改后值',
  ip VARCHAR(50),
  status TINYINT DEFAULT 1 COMMENT '1成功 0失败',
  error_msg TEXT,
  operation_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_system_config: 系统配置
CREATE TABLE IF NOT EXISTS t_system_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL UNIQUE,
  config_value VARCHAR(1000),
  config_type VARCHAR(20) DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/JSON',
  description VARCHAR(200),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_inventory_check: 盘点记录
CREATE TABLE IF NOT EXISTS t_inventory_check (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL,
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT 'SOCIAL/CENTRALIZED',
  system_quantity INT COMMENT '系统数量',
  actual_quantity INT COMMENT '实际数量',
  difference INT COMMENT '差异',
  check_date DATE NOT NULL,
  operator_id BIGINT,
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_bed_transfer: 转床记录
CREATE TABLE IF NOT EXISTS t_bed_transfer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  from_bed_id BIGINT COMMENT '原床位ID，首次分配可为空',
  to_bed_id BIGINT NOT NULL,
  transfer_date DATE NOT NULL,
  reason VARCHAR(200),
  operator_id BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_elderly_allergy: 药物过敏记录
CREATE TABLE IF NOT EXISTS t_elderly_allergy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  drug_name VARCHAR(100) NOT NULL,
  allergy_desc VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_elderly_leave: 老人请假记录
CREATE TABLE IF NOT EXISTS t_elderly_leave (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL COMMENT '老人ID',
  start_date DATE NOT NULL COMMENT '请假开始日期',
  end_date DATE COMMENT '预计结束日期',
  reason VARCHAR(200) COMMENT '请假原因',
  status VARCHAR(20) DEFAULT 'ON_LEAVE' COMMENT 'ON_LEAVE请假中/RETURNED已销假/CANCELLED已取消',
  return_date DATE COMMENT '实际返回日期',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 补贴政策
-- ========================================

-- t_subsidy_policy: 补贴政策配置
CREATE TABLE IF NOT EXISTS t_subsidy_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  policy_code VARCHAR(50) NOT NULL COMMENT '策略编码',
  policy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
  category VARCHAR(20) COMMENT '适用人员类别: SOCIAL/WU_BAO/LOW_BAO/ALL',
  disability_level VARCHAR(20) COMMENT '适用失能等级: null=不限/MODERATE/SEVERE',
  calc_type VARCHAR(20) NOT NULL COMMENT 'FIXED_MONTHLY固定月额/DAILY_RATE日补贴/THRESHOLD_DEDUCT满额抵扣',
  amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额',
  threshold_amount DECIMAL(12,2) COMMENT '满额抵扣阈值',
  deduct_amount DECIMAL(12,2) COMMENT '满额抵扣金额',
  pay_target VARCHAR(20) DEFAULT 'ORG' COMMENT '拨付对象: ORG机构/PERSONAL个人',
  min_stay_days INT COMMENT '最低入住天数要求',
  effective_date DATE NOT NULL COMMENT '生效日期',
  expire_date DATE COMMENT '失效日期(null=长期)',
  enabled TINYINT DEFAULT 1,
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 业务表 - 会计凭证
-- ========================================

-- t_accounting_subject: 会计科目
CREATE TABLE IF NOT EXISTS t_accounting_subject (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(10) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL,
  subject_type VARCHAR(20) NOT NULL,
  direction VARCHAR(10) NOT NULL,
  enabled TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0
);

-- t_voucher_header: 凭证主表(头)
CREATE TABLE IF NOT EXISTS t_voucher_header (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  voucher_word VARCHAR(10) NOT NULL DEFAULT '记',
  voucher_no VARCHAR(30) NOT NULL UNIQUE,
  voucher_date DATE NOT NULL,
  attachment_count INT DEFAULT 0,
  description VARCHAR(500),
  status VARCHAR(20) DEFAULT 'DRAFT',
  related_biz_type VARCHAR(30),
  related_biz_id BIGINT,
  creator_id BIGINT NOT NULL,
  reviewer_id BIGINT,
  review_time DATETIME,
  reject_reason VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_voucher_entry: 凭证分录明细表
CREATE TABLE IF NOT EXISTS t_voucher_entry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  voucher_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  summary VARCHAR(200),
  subject_id BIGINT NOT NULL,
  debit_amount DECIMAL(12,2) DEFAULT 0.00,
  credit_amount DECIMAL(12,2) DEFAULT 0.00
);

-- ========== 服务质量评估表(GB/T 43153-2023 第7章) ==========
CREATE TABLE IF NOT EXISTS t_service_assessment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assessment_no VARCHAR(30) NOT NULL UNIQUE,
  assessment_date DATE NOT NULL,
  assessor_name VARCHAR(50),
  assessor_org VARCHAR(100),
  assessment_period VARCHAR(50),
  elderly_id BIGINT,
  elderly_name VARCHAR(50),
  service_address VARCHAR(300),
  agreement_signed TINYINT DEFAULT 0,
  agreement_complete TINYINT DEFAULT 0,
  plan_formulated TINYINT DEFAULT 0,
  plan_matches_needs TINYINT DEFAULT 0,
  agreement_score INT DEFAULT 0,
  service_on_time TINYINT DEFAULT 0,
  staff_identified TINYINT DEFAULT 0,
  risk_informed TINYINT DEFAULT 0,
  service_per_plan TINYINT DEFAULT 0,
  emergency_handled TINYINT DEFAULT 0,
  acceptance_done TINYINT DEFAULT 0,
  fulfillment_score INT DEFAULT 0,
  record_complete TINYINT DEFAULT 0,
  record_timely TINYINT DEFAULT 0,
  record_accurate TINYINT DEFAULT 0,
  record_score INT DEFAULT 0,
  elderly_satisfaction INT DEFAULT 0,
  satisfaction_method VARCHAR(50),
  total_score INT DEFAULT 0,
  grade VARCHAR(20),
  issues_found TEXT,
  improvement_measures TEXT,
  improvement_deadline DATE,
  photo_urls TEXT,
  assessor_signature_url VARCHAR(500),
  org_signature_url VARCHAR(500),
  status VARCHAR(20) DEFAULT 'DRAFT',
  creator_id BIGINT,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_elderly_change_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  field_name VARCHAR(50) NOT NULL COMMENT '变更字段',
  field_label VARCHAR(50) NOT NULL COMMENT '字段中文名',
  old_value VARCHAR(200) COMMENT '旧值',
  new_value VARCHAR(200) COMMENT '新值',
  operator VARCHAR(50) COMMENT '操作人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_elderly_id ON t_elderly_change_log (elderly_id);

-- ========== 支出记录表 ==========
CREATE TABLE IF NOT EXISTS t_expense_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  expense_type VARCHAR(30) NOT NULL COMMENT 'FOOD/MEDICAL/MAINTENANCE/SALARY/UTILITY/SUPPLIES/OTHER',
  supply_category VARCHAR(20) COMMENT '供应类别: SOCIAL/CENTRALIZED',
  amount DECIMAL(12,2) NOT NULL,
  expense_date DATE NOT NULL,
  payee VARCHAR(100) COMMENT '收款方',
  description VARCHAR(500),
  operator_id BIGINT,
  remark VARCHAR(200),
  bank_account_id BIGINT COMMENT '出账银行账户',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 供应商表 ==========
CREATE TABLE IF NOT EXISTS t_supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '供应商名称',
  credit_code VARCHAR(50) COMMENT '统一信用代码',
  contact VARCHAR(50) COMMENT '联系人',
  phone VARCHAR(50) COMMENT '联系电话',
  bank_name VARCHAR(100) COMMENT '开户行',
  bank_account VARCHAR(100) COMMENT '银行账号',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE在册/DISABLED停用',
  org_id BIGINT COMMENT '所属机构',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 采购申请表 ==========
CREATE TABLE IF NOT EXISTS t_purchase_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_no VARCHAR(30) NOT NULL UNIQUE COMMENT '申请编号',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(50) COMMENT '申请人姓名',
  items_json TEXT NOT NULL COMMENT '采购明细JSON',
  total_amount DECIMAL(12,2) NOT NULL COMMENT '采购总额',
  supply_category VARCHAR(20) COMMENT 'SOCIAL/CENTRALIZED',
  allocation_id BIGINT COMMENT '关联五保拨款批次',
  supplier_id BIGINT COMMENT '供应商ID',
  approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  approver_id BIGINT COMMENT '审批人ID',
  approver_name VARCHAR(50) COMMENT '审批人姓名',
  approve_time DATETIME COMMENT '审批时间',
  approve_remark VARCHAR(500) COMMENT '审批备注',
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 到货验收表 ==========
CREATE TABLE IF NOT EXISTS t_purchase_receipt (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  purchase_request_id BIGINT NOT NULL COMMENT '关联采购申请',
  inspector_id BIGINT COMMENT '验收人ID',
  inspector_name VARCHAR(50) COMMENT '验收人姓名',
  inspect_result VARCHAR(20) COMMENT 'PASS合格/FAIL不合格/PENDING待验收',
  actual_quantity INT COMMENT '实收数量',
  receipt_date DATE COMMENT '验收日期',
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 出库审批申请表 ==========
CREATE TABLE IF NOT EXISTS t_outbound_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(50) COMMENT '申请人姓名',
  material_id BIGINT NOT NULL COMMENT '物资ID',
  material_name VARCHAR(100) COMMENT '物资名称',
  quantity INT NOT NULL COMMENT '申请数量',
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT 'SOCIAL/CENTRALIZED',
  department VARCHAR(50) COMMENT '领用部门',
  purpose VARCHAR(200) COMMENT '用途',
  recipient_staff_id BIGINT COMMENT '领用护工ID',
  recipient_name VARCHAR(50) COMMENT '领用人姓名',
  approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  approver_id BIGINT COMMENT '审批人ID',
  approver_name VARCHAR(50) COMMENT '审批人姓名',
  approve_time DATETIME COMMENT '审批时间',
  approve_remark VARCHAR(500) COMMENT '审批备注',
  inventory_out_id BIGINT COMMENT '审批通过后生成的出库记录ID',
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 预算管理表 ==========
CREATE TABLE IF NOT EXISTS t_budget (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  year INT NOT NULL COMMENT '年度',
  month INT NOT NULL COMMENT '月度',
  supply_category VARCHAR(20) NOT NULL COMMENT 'SOCIAL/CENTRALIZED',
  budget_amount DECIMAL(12,2) DEFAULT 0 COMMENT '预算金额',
  used_amount DECIMAL(12,2) DEFAULT 0 COMMENT '已使用金额',
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_budget_year_month_cat ON t_budget(year, month, supply_category);

-- ========== 银行账户表 ==========
CREATE TABLE IF NOT EXISTS t_bank_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
  account_type VARCHAR(20) DEFAULT 'BASIC' COMMENT 'BASIC基本户/GENERAL一般户',
  bank_name VARCHAR(100) COMMENT '开户行',
  account_number VARCHAR(100) COMMENT '账号',
  initial_balance DECIMAL(12,2) DEFAULT 0 COMMENT '初始余额',
  current_balance DECIMAL(12,2) DEFAULT 0 COMMENT '当前余额',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  org_id BIGINT COMMENT '所属机构',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 银行流水表 ==========
CREATE TABLE IF NOT EXISTS t_bank_transaction (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bank_account_id BIGINT NOT NULL COMMENT '银行账户ID',
  transaction_type VARCHAR(20) NOT NULL COMMENT 'INCOME/EXPENSE',
  amount DECIMAL(12,2) NOT NULL COMMENT '金额',
  balance_after DECIMAL(12,2) COMMENT '交易后余额',
  counterparty VARCHAR(100) COMMENT '对方',
  biz_type VARCHAR(30) COMMENT '业务类型',
  biz_id BIGINT COMMENT '关联业务ID',
  receipt_no VARCHAR(50) COMMENT '流水号',
  transaction_date DATE COMMENT '交易日期',
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 五保拨付表 ==========
CREATE TABLE IF NOT EXISTS t_wubao_allocation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  allocate_month VARCHAR(7) NOT NULL COMMENT '拨付月份YYYY-MM',
  elder_count INT NOT NULL COMMENT '老人数量',
  living_fee_per_person DECIMAL(10,2) DEFAULT 0 COMMENT '生活费人均',
  care_fee_per_person DECIMAL(10,2) DEFAULT 0 COMMENT '护理费人均',
  total_amount DECIMAL(12,2) NOT NULL COMMENT '拨付总额',
  payment_record_id BIGINT COMMENT '关联缴费记录',
  bank_transaction_id BIGINT COMMENT '关联银行流水',
  remark VARCHAR(500),
  org_id BIGINT COMMENT '所属机构',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 库存批次追踪表 ==========
CREATE TABLE IF NOT EXISTS t_inventory_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL COMMENT '物资ID',
  batch_no VARCHAR(50) NOT NULL COMMENT '批号',
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT 'SOCIAL/CENTRALIZED',
  quantity INT NOT NULL COMMENT '入库数量',
  remaining_quantity INT NOT NULL COMMENT '剩余数量',
  expiry_date DATE COMMENT '有效期',
  in_date DATE NOT NULL COMMENT '入库日期',
  inventory_in_id BIGINT COMMENT '关联入库记录',
  supplier VARCHAR(100) COMMENT '供应商',
  operator_id BIGINT COMMENT '操作人',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== 用药管理相关表 ==========

-- t_medication_plan: 用药计划（多药品改造后：药品字段下沉到 t_medication_plan_item）
CREATE TABLE IF NOT EXISTS t_medication_plan (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL COMMENT '老人ID',
  drug_id BIGINT COMMENT '药品ID（已迁移到子表，保留冗余）',
  drug_name VARCHAR(100) COMMENT '药品名称（已迁移到子表，保留冗余）',
  dosage VARCHAR(50) COMMENT '剂量（已迁移到子表，保留冗余）',
  dosage_unit VARCHAR(20) COMMENT '单位：mg/片/ml/粒（已迁移到子表，保留冗余）',
  frequency_type VARCHAR(20) COMMENT 'MULTI_DAILY/N_DAYS/PRN/ONCE',
  interval_days INT DEFAULT 1 COMMENT '间隔天数',
  time_slots VARCHAR(50) NOT NULL COMMENT '服药时段JSON: ["MORNING","EVENING"]',
  start_date DATE NOT NULL COMMENT '开始日期',
  end_date DATE COMMENT '结束日期（null=长期）',
  total_quantity DECIMAL(10,2) COMMENT '发药总量（已迁移到子表，保留冗余）',
  dosage_per_time VARCHAR(50) COMMENT '每次用量数值（已迁移到子表，保留冗余）',
  times_per_day INT DEFAULT 1 COMMENT '每天服用次数',
  depletion_date DATE COMMENT '预计耗尽日期（已迁移到子表，保留冗余）',
  instructions VARCHAR(500) COMMENT '服药说明',
  prescriber_name VARCHAR(50) COMMENT '处方医生',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/COMPLETED/STOPPED',
  operator_id BIGINT COMMENT '操作人',
  remark VARCHAR(200),
  org_id BIGINT,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- t_medication_plan_item: 用药计划药品明细
CREATE TABLE IF NOT EXISTS t_medication_plan_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL COMMENT '关联用药计划ID',
  drug_id BIGINT NOT NULL COMMENT '药品ID',
  drug_name VARCHAR(100) NOT NULL COMMENT '药品名称（冗余）',
  dosage VARCHAR(50) COMMENT '用量说明',
  dosage_unit VARCHAR(20) COMMENT '单位：mg/片/ml/粒',
  total_quantity DECIMAL(10,2) COMMENT '发药总量',
  dosage_per_time VARCHAR(50) COMMENT '每次用量数值',
  depletion_date DATE COMMENT '该药品预计耗尽日期',
  org_id BIGINT,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_medication_record: 用药执行记录
CREATE TABLE IF NOT EXISTS t_medication_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT COMMENT '关联用药计划',
  elderly_id BIGINT NOT NULL COMMENT '老人ID',
  drug_id BIGINT COMMENT '药品ID',
  drug_name VARCHAR(100) COMMENT '药品名称（冗余）',
  dosage VARCHAR(50) COMMENT '用量',
  record_date DATE COMMENT '服药日期',
  time_slot VARCHAR(20) COMMENT 'MORNING/AFTERNOON/EVENING/BEDTIME',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/DONE/SKIPPED/MISSED/REFUSED',
  executor_id BIGINT COMMENT '执行护工ID',
  executor_name VARCHAR(50) COMMENT '执行护工姓名',
  executed_at DATETIME COMMENT '实际执行时间',
  skip_reason VARCHAR(200) COMMENT '跳过/漏服/拒服原因',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
