-- ========================================================================
-- Phase 2 & 3: 仓库管理合规整改 - Schema迁移
-- Tasks 4-6: 供应商/采购/验收/出库审批/预算/批次追踪
-- 
-- 前置条件:
--   1. phase1_warehouse_schema.sql 已执行
--   2. warehouse_enhancement.sql 已执行 (t_inventory_in/t_inventory_out 已有 supply_category)
--
-- 执行前请备份数据库
-- ========================================================================

-- 1. Supplier master data
CREATE TABLE IF NOT EXISTS t_supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL COMMENT '供应商名称',
  credit_code VARCHAR(50) COMMENT '统一信用代码',
  contact VARCHAR(100) COMMENT '联系人',
  phone VARCHAR(30) COMMENT '电话',
  bank_name VARCHAR(200) COMMENT '开户行',
  bank_account VARCHAR(50) COMMENT '银行账号',
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED',
  deleted INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商主数据';

-- 2. Purchase request
CREATE TABLE IF NOT EXISTS t_purchase_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_no VARCHAR(50) COMMENT '申请编号',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(100) COMMENT '申请人姓名',
  items_json TEXT COMMENT '物资清单JSON: [{materialId, materialName, quantity, unitPrice, specification}]',
  total_amount DECIMAL(12,2) DEFAULT 0 COMMENT '总金额',
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT '资金来源: SOCIAL/CENTRALIZED',
  allocation_id BIGINT COMMENT '关联拨款批次ID (CENTRALIZED)',
  supplier_id BIGINT COMMENT '供应商ID',
  approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审批状态: PENDING/APPROVED/REJECTED',
  approver_id BIGINT COMMENT '审批人ID',
  approver_name VARCHAR(100) COMMENT '审批人姓名',
  approve_time DATETIME COMMENT '审批时间',
  approve_remark VARCHAR(500) COMMENT '审批备注',
  remark VARCHAR(500) COMMENT '备注',
  deleted INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请';

-- 3. Purchase receipt (goods receipt / inspection)
CREATE TABLE IF NOT EXISTS t_purchase_receipt (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  purchase_request_id BIGINT NOT NULL COMMENT '关联采购申请ID',
  inspector_id BIGINT COMMENT '验收人ID',
  inspector_name VARCHAR(100) COMMENT '验收人姓名',
  inspect_result VARCHAR(20) DEFAULT 'PASS' COMMENT '验收结果: PASS/FAIL',
  actual_quantity INT COMMENT '实收数量',
  receipt_date DATE COMMENT '验收日期',
  remark VARCHAR(500) COMMENT '备注',
  deleted INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='到货验收';

-- 4. Outbound request (出库审批)
CREATE TABLE IF NOT EXISTS t_outbound_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(100) COMMENT '申请人姓名',
  material_id BIGINT NOT NULL COMMENT '物资ID',
  material_name VARCHAR(200) COMMENT '物资名称',
  quantity INT NOT NULL COMMENT '申请数量',
  supply_category VARCHAR(20) DEFAULT 'CENTRALIZED' COMMENT '供应类别',
  department VARCHAR(100) COMMENT '部门',
  purpose VARCHAR(500) COMMENT '用途',
  recipient_staff_id BIGINT COMMENT '领用护工ID',
  recipient_name VARCHAR(100) COMMENT '领用人姓名',
  approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审批状态: PENDING/APPROVED/REJECTED',
  approver_id BIGINT COMMENT '审批人ID',
  approver_name VARCHAR(100) COMMENT '审批人姓名',
  approve_time DATETIME COMMENT '审批时间',
  approve_remark VARCHAR(500) COMMENT '审批备注',
  inventory_out_id BIGINT COMMENT '审批通过后生成的出库记录ID',
  remark VARCHAR(500) COMMENT '备注',
  deleted INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库申请审批';

-- 5. Budget management
CREATE TABLE IF NOT EXISTS t_budget (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  year INT NOT NULL COMMENT '年度',
  month INT COMMENT '月份 (null=年度预算)',
  supply_category VARCHAR(20) NOT NULL COMMENT '资金类别: SOCIAL/CENTRALIZED',
  budget_amount DECIMAL(12,2) NOT NULL COMMENT '预算金额',
  used_amount DECIMAL(12,2) DEFAULT 0 COMMENT '已使用金额',
  remark VARCHAR(500) COMMENT '备注',
  deleted INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_year_month_category (year, month, supply_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算管理';

-- 6. Inventory batch tracking
CREATE TABLE IF NOT EXISTS t_inventory_batch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL COMMENT '物资ID',
  batch_no VARCHAR(100) COMMENT '批次号',
  supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT '供应类别',
  quantity INT DEFAULT 0 COMMENT '批次数量',
  remaining_quantity INT DEFAULT 0 COMMENT '剩余数量',
  expiry_date DATE COMMENT '有效期',
  in_date DATE COMMENT '入库日期',
  inventory_in_id BIGINT COMMENT '关联入库记录ID',
  remark VARCHAR(500) COMMENT '备注',
  deleted INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存批次追踪';

-- 7. t_inventory_in 扩展: 采购验收入库模式
ALTER TABLE t_inventory_in ADD COLUMN purchase_receipt_id BIGINT COMMENT '关联验收单ID (从采购单入库)' AFTER allocation_id;
ALTER TABLE t_inventory_in ADD COLUMN in_mode VARCHAR(20) DEFAULT 'DIRECT' COMMENT '入库模式: DIRECT/FROM_PURCHASE' AFTER purchase_receipt_id;

-- ========================================================================
-- Indexes for foreign keys and common query patterns
-- ========================================================================

-- Supplier
ALTER TABLE t_supplier ADD INDEX idx_supplier_status (status);

-- Purchase request
ALTER TABLE t_purchase_request ADD INDEX idx_pr_applicant (applicant_id);
ALTER TABLE t_purchase_request ADD INDEX idx_pr_supplier (supplier_id);
ALTER TABLE t_purchase_request ADD INDEX idx_pr_allocation (allocation_id);
ALTER TABLE t_purchase_request ADD INDEX idx_pr_status (approval_status);
ALTER TABLE t_purchase_request ADD INDEX idx_pr_supply_category (supply_category);
ALTER TABLE t_purchase_request ADD INDEX idx_pr_request_no (request_no);

-- Purchase receipt
ALTER TABLE t_purchase_receipt ADD INDEX idx_receipt_request (purchase_request_id);
ALTER TABLE t_purchase_receipt ADD INDEX idx_receipt_inspector (inspector_id);
ALTER TABLE t_purchase_receipt ADD INDEX idx_receipt_result (inspect_result);

-- Outbound request
ALTER TABLE t_outbound_request ADD INDEX idx_or_applicant (applicant_id);
ALTER TABLE t_outbound_request ADD INDEX idx_or_material (material_id);
ALTER TABLE t_outbound_request ADD INDEX idx_or_recipient (recipient_staff_id);
ALTER TABLE t_outbound_request ADD INDEX idx_or_status (approval_status);
ALTER TABLE t_outbound_request ADD INDEX idx_or_supply_category (supply_category);
ALTER TABLE t_outbound_request ADD INDEX idx_or_inventory_out (inventory_out_id);

-- Budget
ALTER TABLE t_budget ADD INDEX idx_budget_year_category (year, supply_category);
ALTER TABLE t_budget ADD INDEX idx_budget_month (year, month, supply_category);

-- Inventory batch
ALTER TABLE t_inventory_batch ADD INDEX idx_batch_material (material_id);
ALTER TABLE t_inventory_batch ADD INDEX idx_batch_supply_category (supply_category);
ALTER TABLE t_inventory_batch ADD INDEX idx_batch_no (batch_no);
ALTER TABLE t_inventory_batch ADD INDEX idx_batch_expiry (expiry_date);
ALTER TABLE t_inventory_batch ADD INDEX idx_batch_inventory_in (inventory_in_id);

-- Inventory in extended indexes
ALTER TABLE t_inventory_in ADD INDEX idx_inventory_in_receipt (purchase_receipt_id);
ALTER TABLE t_inventory_in ADD INDEX idx_inventory_in_mode (in_mode);
