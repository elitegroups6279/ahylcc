-- 用药计划多药品子表 DDL
CREATE TABLE IF NOT EXISTS t_medication_plan_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '关联用药计划ID',
    drug_id BIGINT NOT NULL COMMENT '药品ID',
    drug_name VARCHAR(100) NOT NULL COMMENT '药品名称（冗余）',
    dosage VARCHAR(50) COMMENT '用量说明，如饭后1片',
    dosage_unit VARCHAR(20) COMMENT '单位：mg/片/ml/粒',
    total_quantity DECIMAL(10,2) COMMENT '发药总量',
    dosage_per_time VARCHAR(50) COMMENT '每次用量数值',
    depletion_date DATE COMMENT '该药品预计耗尽日期',
    org_id BIGINT,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan (plan_id),
    INDEX idx_drug (drug_id)
) COMMENT '用药计划药品明细';

-- 数据迁移: 将现有 plan 的 drug 字段迁移到 item 表
INSERT INTO t_medication_plan_item (plan_id, drug_id, drug_name, dosage, dosage_unit, total_quantity, dosage_per_time, depletion_date, org_id, deleted, create_time, update_time)
SELECT id, drug_id, drug_name, dosage, dosage_unit, total_quantity, dosage_per_time, depletion_date, org_id, deleted, create_time, update_time
FROM t_medication_plan
WHERE deleted = 0;

-- 验证迁移结果
SELECT 'migration_check' AS step,
       (SELECT COUNT(*) FROM t_medication_plan WHERE deleted = 0) AS plan_count,
       (SELECT COUNT(*) FROM t_medication_plan_item WHERE deleted = 0) AS item_count;
