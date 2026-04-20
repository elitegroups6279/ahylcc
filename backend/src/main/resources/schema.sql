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
  disability_level VARCHAR(20) DEFAULT 'SELF_CARE' COMMENT '失能等级: SELF_CARE自理/MILD轻度/MODERATE中度/SEVERE重度',
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
  remark VARCHAR(200),
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

-- t_stock: 库存
CREATE TABLE IF NOT EXISTS t_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL UNIQUE,
  quantity INT DEFAULT 0,
  total_value DECIMAL(12,2) DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_inventory_in: 入库记录
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
CREATE INDEX IF NOT EXISTS idx_elderly_id ON t_elderly_change_log (elderly_id);

-- ========== 支出记录表 ==========
CREATE TABLE IF NOT EXISTS t_expense_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  expense_type VARCHAR(30) NOT NULL COMMENT 'FOOD/MEDICAL/MAINTENANCE/SALARY/UTILITY/PURCHASE/OTHER',
  amount DECIMAL(12,2) NOT NULL,
  expense_date DATE NOT NULL,
  payee VARCHAR(100) COMMENT '收款方',
  description VARCHAR(500),
  operator_id BIGINT,
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
