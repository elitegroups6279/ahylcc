-- 种子数据：默认银行账户（基本户 + 一般户）
-- 仅在表为空时插入，避免重复
INSERT INTO t_bank_account (account_name, account_type, bank_name, account_number, initial_balance, current_balance, org_id, status, deleted)
SELECT '鸿福养老基本户', 'BASIC', '', '20000406299010300000018', 0, 0, 1, 'ACTIVE', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_bank_account WHERE account_type = 'BASIC' AND org_id = 1 AND deleted = 0);

INSERT INTO t_bank_account (account_name, account_type, bank_name, account_number, initial_balance, current_balance, org_id, status, deleted)
SELECT '鸿福养老一般户', 'GENERAL', '', '176695001544', 0, 0, 1, 'ACTIVE', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_bank_account WHERE account_type = 'GENERAL' AND org_id = 1 AND deleted = 0);
