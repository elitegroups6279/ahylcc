-- 修复多药品改造后主表旧字段 NOT NULL 导致插入失败的问题
-- 将 t_medication_plan 中已下沉到子表的药品字段改为可空
ALTER TABLE t_medication_plan
  MODIFY COLUMN drug_id BIGINT NULL COMMENT '药品ID（已迁移到子表，保留冗余）',
  MODIFY COLUMN drug_name VARCHAR(100) NULL COMMENT '药品名称（已迁移到子表，保留冗余）',
  MODIFY COLUMN dosage VARCHAR(50) NULL COMMENT '用量，如30mg、1片（已迁移到子表，保留冗余）',
  MODIFY COLUMN frequency_type VARCHAR(20) NULL COMMENT 'MULTI_DAILY/N_DAYS/PRN/ONCE';
