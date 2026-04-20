# P2~P5 开发规格说明

## 背景
本规格覆盖《REQUIREMENTS.md》中的 P2~P5：在已完成 P0~P1（权限体系、系统管理、通知提醒、入住/护工/缴费/报账最小可用）的基础上，继续实现扩展业务、财务完整、集成测试与上线部署。

## 范围

### P2 扩展业务（最小可用）
- 仓库管理：物资档案、入库、出库、库存看板、盘点
- 药物管理：药品档案、批次入库、发药单/发药记录、近效期预警
- 打卡记录：打卡流水查询与补录
- 上门服务：预约单、服务记录、结算状态

### P3 财务完整
- 凭证管理：凭证流水、类型/类别、附件 URL、按月筛选
- 报账审批：流程状态流转完善、审批/驳回/支付记录完善
- 报表导出：按文档列出的核心报表导出（先 Excel）

### P4 集成测试
- 全流程联调：入住→缴费→预警→退住/转床；仓库/药物/上门服务核心链路
- 性能与稳定性：接口 P95、基础并发压测脚本
- 安全扫描与加固：XSS/SQLi、敏感字段处理、权限校验补齐

### P5 上线部署
- 生产部署：Nginx + 前端静态 + 后端服务
- 数据迁移：H2→MySQL（若仍存在本地演示数据）
- 运维与培训：环境变量、备份策略、常见问题手册

## 统一约定
- API 前缀：后端统一 `/api/**`，返回 `ApiResponse` 结构
- 分页：统一 `PageResult { page, pageSize, total, list }`
- 权限：前端路由守卫 + 后端接口逐步补齐 `@PreAuthorize(hasAuthority(...))`
- 时间格式：前端 `YYYY-MM-DD` / `YYYY-MM-DD HH:mm:ss`
- 附件：先以 URL 字符串/JSON 存储，后续对接 OSS 再扩展上传接口

## 数据模型（已存在于 schema.sql 的表）
- 仓库：`t_material`、`t_stock`、`t_inventory_in`、`t_inventory_out`、`t_inventory_check`
- 药物：`t_drug`、`t_drug_batch`、`t_dispense_order`、`t_dispense_record`
- 打卡：`t_attendance`
- 上门服务：`t_home_service_order`、`t_home_service_record`
- 财务：`t_voucher`（凭证）、（已实现）`t_reimbursement`（报账）

## P2 功能规格（接口级）

### 仓库管理
- 物资档案
  - `GET /api/warehouse/materials`（分页+关键词+类别）
  - `POST /api/warehouse/materials`
  - `PUT /api/warehouse/materials/{id}`
  - `DELETE /api/warehouse/materials/{id}`
- 库存看板
  - `GET /api/warehouse/stocks`（分页+预警筛选：quantity <= warning_threshold）
- 入库
  - `GET /api/warehouse/in`（分页）
  - `POST /api/warehouse/in`（写入入库记录 + 库存加数量/金额）
- 出库
  - `GET /api/warehouse/out`（分页）
  - `POST /api/warehouse/out`（写出库记录 + 库存减数量；不足时报错）
- 盘点
  - `GET /api/warehouse/checks`（分页）
  - `POST /api/warehouse/checks`（记录 system/actual/diff）

### 药物管理
- 药品档案
  - `GET /api/pharmacy/drugs`（分页+关键词）
  - `POST /api/pharmacy/drugs`
  - `PUT /api/pharmacy/drugs/{id}`
  - `DELETE /api/pharmacy/drugs/{id}`
- 批次入库
  - `GET /api/pharmacy/batches`（分页+drugId）
  - `POST /api/pharmacy/batches`（新增批次，remaining=quantity）
- 发药
  - `GET /api/pharmacy/dispense/orders`（分页）
  - `POST /api/pharmacy/dispense/orders`（创建发药单）
  - `POST /api/pharmacy/dispense/orders/{id}/confirm`（出库扣减批次 remaining，生成 dispense_record）
- 近效期预警
  - `GET /api/pharmacy/expiry-warnings`（默认 30 天，可按系统配置）

### 打卡记录
- `GET /api/staff/attendance`（分页+staffId+日期范围）
- `POST /api/staff/attendance`（补录 clock-in/out）

### 上门服务
- 预约单
  - `GET /api/home-service/orders`（分页+状态）
  - `POST /api/home-service/orders`
  - `PUT /api/home-service/orders/{id}`（状态流转：PENDING→CONFIRMED→CANCELLED）
- 服务记录
  - `GET /api/home-service/records`（分页+orderId）
  - `POST /api/home-service/records`（录入实际服务、签字、评分、金额）

## P3 功能规格（接口级）
- 凭证管理
  - `GET /api/finance/vouchers`（分页+月份+类型）
  - `POST /api/finance/vouchers`
  - `DELETE /api/finance/vouchers/{id}`
- 报账审批完善
  - 维持已实现接口，补齐审批人/审核人字段、状态机约束、以及通知铃铛数据源切到真实待审批
- 报表导出（Excel）
  - `GET /api/reports/fee-summary`（按月/老人）
  - `GET /api/reports/elderly-roster`（在住花名册）
  - `GET /api/reports/staff-attendance`（按月）
  - `GET /api/reports/material-consumption`（按月/部门）

## P4/P5 交付物
- P4：测试用例清单、压测脚本（JMeter/简单脚本）、安全自检清单与整改项
- P5：部署文档（Nginx/环境变量/启动顺序）、数据库迁移说明、备份策略、运维手册
