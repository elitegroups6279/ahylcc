-- 银行账户表
CREATE TABLE IF NOT EXISTS t_bank_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
  account_type VARCHAR(20) NOT NULL COMMENT 'BASIC(基本户)/GENERAL(一般户)',
  bank_name VARCHAR(100) COMMENT '开户银行',
  account_number VARCHAR(50) COMMENT '银行账号',
  initial_balance DECIMAL(12,2) DEFAULT 0 COMMENT '期初余额',
  current_balance DECIMAL(12,2) DEFAULT 0 COMMENT '当前余额',
  org_id BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FROZEN/CLOSED',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '机构银行账户';

-- 银行流水表
CREATE TABLE IF NOT EXISTS t_bank_transaction (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bank_account_id BIGINT NOT NULL COMMENT '关联银行账户',
  transaction_type VARCHAR(20) NOT NULL COMMENT 'INCOME/EXPENSE',
  amount DECIMAL(12,2) NOT NULL,
  balance_after DECIMAL(12,2) COMMENT '交易后余额',
  counterparty VARCHAR(100) COMMENT '对方户名',
  transaction_date DATE NOT NULL,
  biz_type VARCHAR(50) COMMENT '业务类型',
  biz_id BIGINT COMMENT '关联业务记录ID',
  description VARCHAR(500),
  receipt_no VARCHAR(100) COMMENT '银行回单号',
  operator_id BIGINT,
  org_id BIGINT NOT NULL,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_bank_account_id (bank_account_id),
  INDEX idx_transaction_date (transaction_date),
  INDEX idx_biz (biz_type, biz_id)
) COMMENT '银行流水';

-- 修改现有表
ALTER TABLE t_payment_record ADD COLUMN bank_account_id BIGINT COMMENT '入账银行账户';
ALTER TABLE t_expense_record ADD COLUMN bank_account_id BIGINT COMMENT '出账银行账户';
ALTER TABLE t_inventory_in ADD COLUMN expense_record_id BIGINT COMMENT '关联支出记录';
