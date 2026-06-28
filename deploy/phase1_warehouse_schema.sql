-- ========================================================================
-- Phase 1: 仓库管理合规整改 - Schema迁移
-- Tasks 1-3: DDL, Entity/DTO, Mapper 基础层
-- 
-- 前置条件:
--   1. warehouse_enhancement.sql 已执行 (t_inventory_in/t_inventory_out 已有 supply_category)
--   2. t_stock.material_id 的 UNIQUE 约束为列级定义, 索引名为 'material_id'
--
-- 执行前请备份数据库
-- ========================================================================

-- 1. t_stock: 增加 supply_category 字段并修改 UNIQUE 约束
--    当前: material_id BIGINT NOT NULL UNIQUE (列级约束, 索引名 = material_id)
--    目标: UNIQUE(material_id, supply_category) 复合唯一索引
ALTER TABLE t_stock ADD COLUMN supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT '供应类别: SOCIAL/CENTRALIZED' AFTER material_id;
UPDATE t_stock SET supply_category = 'SOCIAL' WHERE supply_category IS NULL;

-- 删除旧的列级 UNIQUE 索引 (索引名 = 列名 material_id)
ALTER TABLE t_stock DROP INDEX material_id;

-- 创建新的复合唯一索引
ALTER TABLE t_stock ADD UNIQUE INDEX uk_material_category (material_id, supply_category);

-- 供应类别查询索引
ALTER TABLE t_stock ADD INDEX idx_supply_category (supply_category);

-- 2. t_inventory_in: 增加 allocation_id (关联集中供养拨款批次)
--    注意: supply_category 已由 warehouse_enhancement.sql 添加
ALTER TABLE t_inventory_in ADD COLUMN allocation_id BIGINT COMMENT '关联集中供养拨款批次ID' AFTER supply_category;
ALTER TABLE t_inventory_in ADD INDEX idx_allocation_id (allocation_id);

-- 3. t_inventory_out: 增加领用人/受益人字段
--    注意: supply_category 已由 warehouse_enhancement.sql 添加
ALTER TABLE t_inventory_out ADD COLUMN recipient_staff_id BIGINT COMMENT '领用护工ID' AFTER supply_category;
ALTER TABLE t_inventory_out ADD COLUMN recipient_name VARCHAR(100) COMMENT '领用护工姓名' AFTER recipient_staff_id;
ALTER TABLE t_inventory_out ADD COLUMN recipient_sign_url VARCHAR(500) COMMENT '受益人签字URL' AFTER recipient_name;
ALTER TABLE t_inventory_out ADD INDEX idx_recipient_staff (recipient_staff_id);

-- 4. t_inventory_check: 增加 supply_category
ALTER TABLE t_inventory_check ADD COLUMN supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT '供应类别: SOCIAL/CENTRALIZED' AFTER material_id;
UPDATE t_inventory_check SET supply_category = 'SOCIAL' WHERE supply_category IS NULL;
