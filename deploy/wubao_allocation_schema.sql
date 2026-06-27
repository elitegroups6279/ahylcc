CREATE TABLE IF NOT EXISTS t_wubao_allocation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  allocate_month VARCHAR(7) NOT NULL COMMENT '拨付月份 YYYY-MM',
  elder_count INT NOT NULL COMMENT '五保人数',
  living_fee_per_person DECIMAL(12,2) DEFAULT 800 COMMENT '生活费标准/人/月',
  care_fee_per_person DECIMAL(12,2) DEFAULT 2000 COMMENT '照料补助标准/人/月',
  total_amount DECIMAL(12,2) NOT NULL COMMENT '总拨付金额',
  counterparty VARCHAR(100) DEFAULT '民政局' COMMENT '拨付来源',
  receipt_no VARCHAR(100) COMMENT '银行回单号',
  payment_record_id BIGINT COMMENT '关联收入记录',
  bank_transaction_id BIGINT COMMENT '关联银行流水',
  operator_id BIGINT,
  org_id BIGINT NOT NULL,
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_month (allocate_month)
) COMMENT '五保拨付记录';
