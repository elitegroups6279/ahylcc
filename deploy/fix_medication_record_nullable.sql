-- 修复用药执行记录表 NOT NULL 约束导致新增用药计划时 backfillRecords 失败的问题
-- 问题：多药品改造后，backfillRecords 不再保证 dosage 字段非空，MyBatis-Plus 会跳过 null 字段
-- 导致 INSERT t_medication_record 时 dosage 列取默认值失败（NOT NULL 无 default）
-- 解决：将可能被跳过/为空的列改为 DEFAULT NULL
ALTER TABLE t_medication_record
    MODIFY COLUMN drug_id BIGINT DEFAULT NULL,
    MODIFY COLUMN drug_name VARCHAR(100) DEFAULT NULL,
    MODIFY COLUMN dosage VARCHAR(50) DEFAULT NULL,
    MODIFY COLUMN time_slot VARCHAR(20) DEFAULT NULL,
    MODIFY COLUMN status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/DONE/SKIPPED/MISSED/REFUSED';
