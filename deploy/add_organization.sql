-- Organization table and seed data
CREATE TABLE IF NOT EXISTS t_organization (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  org_code VARCHAR(50) NOT NULL UNIQUE COMMENT '机构编码',
  org_name VARCHAR(100) NOT NULL COMMENT '机构名称',
  address VARCHAR(300) COMMENT '地址',
  phone VARCHAR(50) COMMENT '联系电话',
  contact_person VARCHAR(50) COMMENT '联系人',
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO t_organization (id, org_code, org_name, address, phone, contact_person)
VALUES (1, 'DEFAULT', '默认机构', '', '', '')
ON DUPLICATE KEY UPDATE org_name=org_name;

-- Menu entry for 机构管理
INSERT INTO t_menu (parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, visible, status)
SELECT id, '机构管理', 1, '/system/organizations', 'system/Organizations', 'system:organizations', 'Office', 5, 1, 1
FROM t_menu WHERE menu_name='系统管理' AND menu_type=0 LIMIT 1;

-- Button-level permissions
INSERT INTO t_menu (parent_id, menu_name, menu_type, permission, sort_order, visible, status)
SELECT id, '新增机构', 2, 'system:organizations:add', 1, 1, 1 FROM t_menu WHERE menu_name='机构管理' AND menu_type=1;

INSERT INTO t_menu (parent_id, menu_name, menu_type, permission, sort_order, visible, status)
SELECT id, '编辑机构', 2, 'system:organizations:edit', 2, 1, 1 FROM t_menu WHERE menu_name='机构管理' AND menu_type=1;

INSERT INTO t_menu (parent_id, menu_name, menu_type, permission, sort_order, visible, status)
SELECT id, '删除机构', 2, 'system:organizations:delete', 3, 1, 1 FROM t_menu WHERE menu_name='机构管理' AND menu_type=1;

-- Assign all new menus to SUPER_ADMIN role (role_id=1)
INSERT IGNORE INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu WHERE permission IN ('system:organizations', 'system:organizations:add', 'system:organizations:edit', 'system:organizations:delete');
