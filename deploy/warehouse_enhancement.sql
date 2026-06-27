-- Add supply_category to inventory tables
ALTER TABLE t_inventory_in ADD COLUMN supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT '供应类别: SOCIAL(社会化物资)/CENTRALIZED(集中供养物资)';
ALTER TABLE t_inventory_out ADD COLUMN supply_category VARCHAR(20) DEFAULT 'SOCIAL' COMMENT '供应类别: SOCIAL(社会化物资)/CENTRALIZED(集中供养物资)';
-- Add specification to inventory_out
ALTER TABLE t_inventory_out ADD COLUMN specification VARCHAR(100) COMMENT '规格';
