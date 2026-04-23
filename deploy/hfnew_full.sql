-- ========================================
-- 养老企业管理系统 v1.0.0 完整数据库
-- 适用于: 腾讯云 MySQL / TDSQL-C (MySQL 8.0 兼容)
-- 数据库: hfcc
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 导入方式: mysql -h <host> -P <port> -u <user> -p <db> < hfnew_full.sql
-- 或在腾讯云控制台「数据库管理」→「SQL导入」直接上传
-- ========================================

USE hfcc;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 权限相关表
-- ========================================

-- t_user: 系统用户
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- t_role: 角色
DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL UNIQUE,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

-- t_menu: 菜单
DROP TABLE IF EXISTS t_menu;
CREATE TABLE t_menu (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单';

-- t_user_role: 用户-角色关联
DROP TABLE IF EXISTS t_user_role;
CREATE TABLE t_user_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联';

-- t_role_menu: 角色-菜单关联
DROP TABLE IF EXISTS t_role_menu;
CREATE TABLE t_role_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  UNIQUE (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联';

-- t_organization: 机构表
DROP TABLE IF EXISTS t_organization;
CREATE TABLE t_organization (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构表';

-- ========================================
-- 业务表 - 入住管理
-- ========================================

-- t_bed: 床位
DROP TABLE IF EXISTS t_bed;
CREATE TABLE t_bed (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='床位';

-- t_elderly: 老人
DROP TABLE IF EXISTS t_elderly;
CREATE TABLE t_elderly (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人';

-- t_elderly_contact: 家属联系人
DROP TABLE IF EXISTS t_elderly_contact;
CREATE TABLE t_elderly_contact (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  relationship VARCHAR(20) COMMENT '子女/配偶/其他',
  phone VARCHAR(50) NOT NULL,
  is_emergency TINYINT DEFAULT 0 COMMENT '是否紧急联系人',
  sort_order INT DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家属联系人';

-- ========================================
-- 业务表 - 护工/人事
-- ========================================

-- t_staff: 护工
DROP TABLE IF EXISTS t_staff;
CREATE TABLE t_staff (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='护工';

-- t_staff_assignment: 护工-老人关联
DROP TABLE IF EXISTS t_staff_assignment;
CREATE TABLE t_staff_assignment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  assign_type VARCHAR(20) DEFAULT 'PRIMARY' COMMENT 'PRIMARY主责/SECONDARY备用',
  start_time DATETIME NOT NULL,
  end_time DATETIME,
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='护工-老人关联';

-- t_attendance: 打卡记录
DROP TABLE IF EXISTS t_attendance;
CREATE TABLE t_attendance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  clock_in_time DATETIME,
  clock_out_time DATETIME,
  status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL正常/LATE迟到/EARLY早退/ABSENT缺勤',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录';

-- t_schedule: 排班
DROP TABLE IF EXISTS t_schedule;
CREATE TABLE t_schedule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  shift_type VARCHAR(20) NOT NULL COMMENT 'MORNING早班/AFTERNOON中班/NIGHT夜班',
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班';

-- ========================================
-- 业务表 - 财务
-- ========================================

-- t_fee_account: 费用账户
DROP TABLE IF EXISTS t_fee_account;
CREATE TABLE t_fee_account (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用账户';

-- t_fee_bill: 月账单
DROP TABLE IF EXISTS t_fee_bill;
CREATE TABLE t_fee_bill (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月账单';

-- t_payment_record: 缴费记录
DROP TABLE IF EXISTS t_payment_record;
CREATE TABLE t_payment_record (
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
  org_id BIGINT COMMENT '所属机构ID',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缴费记录';

-- t_voucher: 凭证
DROP TABLE IF EXISTS t_voucher;
CREATE TABLE t_voucher (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='凭证';

-- t_reimbursement: 报账
DROP TABLE IF EXISTS t_reimbursement;
CREATE TABLE t_reimbursement (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报账';

-- ========================================
-- 业务表 - 仓库
-- ========================================

-- t_material: 物资
DROP TABLE IF EXISTS t_material;
CREATE TABLE t_material (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) COMMENT '生活用品/护理耗材/医疗设备/清洁消毒/办公用品',
  specification VARCHAR(100) COMMENT '规格型号',
  unit VARCHAR(20) COMMENT '单位',
  warning_threshold INT DEFAULT 10 COMMENT '库存预警阈值',
  description VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物资';

-- t_stock: 库存
DROP TABLE IF EXISTS t_stock;
CREATE TABLE t_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL UNIQUE,
  quantity INT DEFAULT 0,
  total_value DECIMAL(12,2) DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存';

-- t_inventory_in: 入库记录
DROP TABLE IF EXISTS t_inventory_in;
CREATE TABLE t_inventory_in (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库记录';

-- t_inventory_out: 出库记录
DROP TABLE IF EXISTS t_inventory_out;
CREATE TABLE t_inventory_out (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库记录';

-- ========================================
-- 业务表 - 药物
-- ========================================

-- t_drug: 药品档案
DROP TABLE IF EXISTS t_drug;
CREATE TABLE t_drug (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品档案';

-- t_drug_batch: 药品批次
DROP TABLE IF EXISTS t_drug_batch;
CREATE TABLE t_drug_batch (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品批次';

-- t_dispense_order: 发药单
DROP TABLE IF EXISTS t_dispense_order;
CREATE TABLE t_dispense_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  order_date DATE NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/DISPENSED/CANCELLED',
  operator_id BIGINT,
  remark VARCHAR(200),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发药单';

-- t_dispense_record: 发药记录
DROP TABLE IF EXISTS t_dispense_record;
CREATE TABLE t_dispense_record (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发药记录';

-- ========================================
-- 业务表 - 上门服务
-- ========================================

-- t_service_item: 服务项目
DROP TABLE IF EXISTS t_service_item;
CREATE TABLE t_service_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) COMMENT '服务大类',
  price DECIMAL(10,2) NOT NULL COMMENT '收费标准',
  unit VARCHAR(20) COMMENT '计费单位(次/小时)',
  description VARCHAR(500),
  status TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务项目';

-- t_home_service_order: 上门服务订单
DROP TABLE IF EXISTS t_home_service_order;
CREATE TABLE t_home_service_order (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上门服务订单';

-- t_home_service_record: 服务记录
DROP TABLE IF EXISTS t_home_service_record;
CREATE TABLE t_home_service_record (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务记录';

-- ========================================
-- 系统表
-- ========================================

-- t_operation_log: 操作日志
DROP TABLE IF EXISTS t_operation_log;
CREATE TABLE t_operation_log (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- t_system_config: 系统配置
DROP TABLE IF EXISTS t_system_config;
CREATE TABLE t_system_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL UNIQUE,
  config_value VARCHAR(1000),
  config_type VARCHAR(20) DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/JSON',
  description VARCHAR(200),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- t_inventory_check: 盘点记录
DROP TABLE IF EXISTS t_inventory_check;
CREATE TABLE t_inventory_check (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点记录';

-- t_bed_transfer: 转床记录
DROP TABLE IF EXISTS t_bed_transfer;
CREATE TABLE t_bed_transfer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  from_bed_id BIGINT COMMENT '原床位ID，首次分配可为空',
  to_bed_id BIGINT NOT NULL,
  transfer_date DATE NOT NULL,
  reason VARCHAR(200),
  operator_id BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转床记录';

-- t_elderly_allergy: 药物过敏记录
DROP TABLE IF EXISTS t_elderly_allergy;
CREATE TABLE t_elderly_allergy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  drug_name VARCHAR(100) NOT NULL,
  allergy_desc VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药物过敏记录';

-- t_elderly_leave: 老人请假记录
DROP TABLE IF EXISTS t_elderly_leave;
CREATE TABLE t_elderly_leave (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL COMMENT '老人ID',
  start_date DATE NOT NULL COMMENT '请假开始日期',
  end_date DATE COMMENT '预计结束日期',
  reason VARCHAR(200) COMMENT '请假原因',
  status VARCHAR(20) DEFAULT 'ON_LEAVE' COMMENT 'ON_LEAVE请假中/RETURNED已销假/CANCELLED已取消',
  return_date DATE COMMENT '实际返回日期',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人请假记录';

-- ========================================
-- 业务表 - 补贴政策
-- ========================================

-- t_subsidy_policy: 补贴政策配置
DROP TABLE IF EXISTS t_subsidy_policy;
CREATE TABLE t_subsidy_policy (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补贴政策配置';

-- ========================================
-- 业务表 - 会计凭证
-- ========================================

-- t_accounting_subject: 会计科目
DROP TABLE IF EXISTS t_accounting_subject;
CREATE TABLE t_accounting_subject (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(10) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL,
  subject_type VARCHAR(20) NOT NULL,
  direction VARCHAR(10) NOT NULL,
  enabled TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计科目';

-- t_voucher_header: 凭证主表(头)
DROP TABLE IF EXISTS t_voucher_header;
CREATE TABLE t_voucher_header (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='凭证主表(头)';

-- t_voucher_entry: 凭证分录明细表
DROP TABLE IF EXISTS t_voucher_entry;
CREATE TABLE t_voucher_entry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  voucher_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  summary VARCHAR(200),
  subject_id BIGINT NOT NULL,
  debit_amount DECIMAL(12,2) DEFAULT 0.00,
  credit_amount DECIMAL(12,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='凭证分录明细表';

-- ========== 服务质量评估表(GB/T 43153-2023 第7章) ==========
DROP TABLE IF EXISTS t_service_assessment;
CREATE TABLE t_service_assessment (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务质量评估表';

-- t_elderly_change_log: 老人信息变更日志
DROP TABLE IF EXISTS t_elderly_change_log;
CREATE TABLE t_elderly_change_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  field_name VARCHAR(50) NOT NULL COMMENT '变更字段',
  field_label VARCHAR(50) NOT NULL COMMENT '字段中文名',
  old_value VARCHAR(200) COMMENT '旧值',
  new_value VARCHAR(200) COMMENT '新值',
  operator VARCHAR(50) COMMENT '操作人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人信息变更日志';
CREATE INDEX idx_elderly_id ON t_elderly_change_log (elderly_id);

-- ========== 支出记录表 ==========
DROP TABLE IF EXISTS t_expense_record;
CREATE TABLE t_expense_record (
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
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支出记录';

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 种子数据
-- 使用 INSERT IGNORE 保证幂等性（已存在则跳过）
-- ========================================

-- 超级管理员用户 (密码: Admin123!)
INSERT IGNORE INTO t_user (id, username, password, real_name, status)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 1);

-- 默认角色
INSERT IGNORE INTO t_role (id, role_name, role_code, description) VALUES
  (1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
  (2, '机构负责人', 'ORG_MANAGER', '可配置系统参数与运营看板'),
  (3, '财务人员', 'FINANCE', '缴费、凭证、报账、月结等'),
  (4, '人事专员', 'HR', '护工档案、排班、打卡管理'),
  (5, '护理主管', 'NURSING_SUPERVISOR', '护工考核、护理计划分配'),
  (6, '护工', 'CAREGIVER', '打卡、护理记录录入'),
  (7, '仓库员', 'WAREHOUSE', '物资入出库登记、库存盘点'),
  (8, '药剂员', 'PHARMACIST', '药品管理、发药登记'),
  (9, '上门服务专员', 'HOME_SERVICE', '预约受理、服务记录提交');

-- 绑定 admin 用户到超级管理员角色
INSERT IGNORE INTO t_user_role (user_id, role_id) VALUES (1, 1);

-- 菜单树 - 一级菜单(目录)
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (1, 0, '首页', 0, '/dashboard', 'pages/Dashboard', 'HomeFilled', 1, NULL),
  (2, 0, '入住管理', 0, '/elderly', NULL, 'User', 2, NULL),
  (3, 0, '人事管理', 0, '/staff', NULL, 'UserFilled', 3, NULL),
  (4, 0, '财务管理', 0, '/finance', NULL, 'Money', 4, NULL),
  (5, 0, '仓库管理', 0, '/warehouse', NULL, 'Box', 5, NULL),
  (6, 0, '药物管理', 0, '/pharmacy', NULL, 'FirstAidKit', 6, NULL),
  (7, 0, '上门服务', 0, '/home-service', NULL, 'Van', 7, NULL),
  (8, 0, '系统管理', 0, '/system', NULL, 'Setting', 8, NULL),
  (9, 0, '报表管理', 0, '/reports', NULL, 'DataAnalysis', 9, NULL);

-- 菜单树 - 二级菜单: 入住管理
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (21, 2, '在住老人列表', 1, '/elderly/list', 'pages/elderly/ElderlyList', NULL, 1, 'elderly:list'),
  (22, 2, '新增入住', 1, '/elderly/add', 'pages/elderly/ElderlyAdd', NULL, 2, 'elderly:add'),
  (23, 2, '退住管理', 1, '/elderly/discharge', 'pages/elderly/ElderlyDischarge', NULL, 3, 'elderly:discharge'),
  (24, 2, '转床管理', 1, '/elderly/transfer', 'pages/elderly/ElderlyTransfer', NULL, 4, 'elderly:transfer');

-- 人事管理子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (31, 3, '护工管理', 1, '/staff/list', 'pages/staff/StaffList', NULL, 1, 'staff:list'),
  (32, 3, '打卡记录', 1, '/staff/attendance', 'pages/staff/Attendance', NULL, 2, 'staff:attendance'),
  (33, 3, '排班管理', 1, '/staff/schedule', 'pages/staff/Schedule', NULL, 3, 'staff:schedule'),
  (34, 3, '员工管理', 1, '/staff/employees', 'pages/staff/Employees', NULL, 4, 'staff:employee');

-- 财务管理子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (41, 4, '缴费管理', 1, '/finance/payment', 'pages/finance/Payment', NULL, 1, 'finance:payment'),
  (42, 4, '凭证管理', 1, '/finance/voucher', 'pages/finance/Voucher', NULL, 2, 'finance:voucher'),
  (43, 4, '报账管理', 1, '/finance/reimbursement', 'pages/finance/Reimbursement', NULL, 3, 'finance:reimbursement'),
  (44, 4, '月度账单', 1, '/finance/bills', 'pages/finance/Bills', NULL, 4, 'finance:bill'),
  (45, 4, '补贴政策', 1, '/finance/subsidy', 'pages/finance/Subsidy', NULL, 5, 'system:config');

-- 报表管理子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (91, 9, '报表导出', 1, '/reports/export', 'pages/reports/Reports', NULL, 1, 'report:export');

-- 仓库管理子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (50, 5, '物资档案', 1, '/warehouse/materials', 'pages/warehouse/Materials', NULL, 0, 'warehouse:materials'),
  (51, 5, '入库管理', 1, '/warehouse/in', 'pages/warehouse/InventoryIn', NULL, 1, 'warehouse:in'),
  (52, 5, '出库管理', 1, '/warehouse/out', 'pages/warehouse/InventoryOut', NULL, 2, 'warehouse:out'),
  (53, 5, '库存看板', 1, '/warehouse/stock', 'pages/warehouse/Stock', NULL, 3, 'warehouse:stock'),
  (54, 5, '盘点管理', 1, '/warehouse/check', 'pages/warehouse/InventoryCheck', NULL, 4, 'warehouse:check');

-- 药物管理子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (61, 6, '药品档案', 1, '/pharmacy/drugs', 'pages/pharmacy/DrugList', NULL, 1, 'pharmacy:drugs'),
  (62, 6, '发药管理', 1, '/pharmacy/dispense', 'pages/pharmacy/Dispense', NULL, 2, 'pharmacy:dispense');

-- 上门服务子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (71, 7, '预约管理', 1, '/home-service/orders', 'pages/home-service/Orders', NULL, 1, 'home-service:orders'),
  (72, 7, '服务记录', 1, '/home-service/records', 'pages/home-service/Records', NULL, 2, 'home-service:records'),
  (73, 7, '服务评估', 1, '/home-service/assessment', 'pages/home-service/Assessment', NULL, 3, 'home-service:assessment');

-- 系统管理子菜单
INSERT IGNORE INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission) VALUES
  (81, 8, '账户管理', 1, '/system/accounts', 'pages/system/Accounts', NULL, 1, 'system:accounts'),
  (82, 8, '角色管理', 1, '/system/roles', 'pages/system/Roles', NULL, 2, 'system:roles'),
  (83, 8, '菜单管理', 1, '/system/menus', 'pages/system/Menus', NULL, 3, 'system:menus'),
  (84, 8, '系统配置', 1, '/system/config', 'pages/system/Config', NULL, 4, 'system:config'),
  (85, 8, '操作日志', 1, '/system/logs', 'pages/system/Logs', NULL, 5, 'system:logs');

-- 超管角色关联所有菜单
INSERT IGNORE INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu;

-- 默认系统配置
INSERT IGNORE INTO t_system_config (id, config_key, config_value, config_type, description) VALUES
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

-- 默认床位数据 (1栋 1F/2F)
INSERT IGNORE INTO t_bed (id, building, floor, room_number, bed_number, status) VALUES
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

-- 示例数据: Dashboard演示
INSERT IGNORE INTO t_elderly (id, unique_no, name, id_card, gender, birth_date, age, admission_date, bed_id, category, enable_long_care, enable_coupon, contract_monthly_fee, status)
VALUES (1, '2026040001', '张大爷', 'ENC(110101199001011234)', 1, '1990-01-01', 66, CURDATE(), 1, 'SOCIAL', 0, 0, 5400.00, 'ACTIVE');

INSERT IGNORE INTO t_fee_account (id, elderly_id, balance, total_charged, total_consumed, warning_status)
VALUES (1, 1, 300.00, 300.00, 0.00, 1);

INSERT IGNORE INTO t_reimbursement (id, applicant_id, amount, reason, status)
VALUES (1, 1, 120.50, '示例：采购消毒用品报账', 'PENDING');
