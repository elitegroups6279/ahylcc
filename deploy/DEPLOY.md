# 部署与运维

## 前置
- 后端：JDK 17 + Maven
- 前端：Node.js 18+（含 npm）
- 数据库：MySQL 8+
- Nginx：用于静态资源与反向代理

## 后端部署（Spring Boot）
### 构建
在 `backend/`：
- `mvn clean package`
- 产物：`backend/target/*.jar`

### 运行
建议用环境变量覆盖配置（示例见 `deploy/backend.env.example`）：
- `MYSQL_HOST / MYSQL_PORT / MYSQL_DATABASE / MYSQL_USERNAME / MYSQL_PASSWORD`
- `APP_JWT_ACCESS_SECRET / APP_JWT_REFRESH_SECRET`
- `APP_SECURITY_ALLOWED_ORIGINS`

启动示例：
- `java -jar target/hfnew-backend-0.0.1-SNAPSHOT.jar`

## 前端部署（Vite）
### 构建
在 `frontend/`：
- `npm install`
- `npm run build`

构建产物在 `frontend/dist/`。

### Nginx
- 将 `frontend/dist/*` 部署到 Nginx 的 `root` 目录（示例配置见 `deploy/nginx.conf`，默认 `/var/www/hfnew`）。
- `/api/` 反向代理到后端 `http://127.0.0.1:8080`。
- SPA 路由需要 `try_files ... /index.html`。

## 数据库初始化/迁移与备份
### 初始化
后端默认 `spring.sql.init.mode=always`，启动时会执行：
- `schema.sql`：建表
- `data-mysql.sql`：种子数据（幂等）

生产建议：
- 首次初始化后，将 `spring.sql.init.mode` 调整为 `never`（或改用独立迁移工具），避免误执行变更。

### 迁移
当前 schema 由 `schema.sql` 维护，建议后续引入迁移工具（Flyway/Liquibase）做版本化管理。

### 备份
示例（按实际账号/库名调整）：
- `mysqldump -h <host> -P <port> -u <user> -p <db> > backup.sql`

恢复：
- `mysql -h <host> -P <port> -u <user> -p <db> < backup.sql`

## 上线验收与常见问题
### 上线验收（最小项）
- 登录/刷新 token 正常
- 权限菜单渲染正常
- 财务：缴费/报账/凭证基本链路可用
- P2：仓库/药物/上门服务页面可用
- 报表导出可下载 xlsx
- 通知铃铛可看到待处理计数（报账/库存预警/近效期/合同到期）

### 常见问题
- 401：确认前端 `VITE_API_BASE_URL` 指向同域或 CORS 已放行 `APP_SECURITY_ALLOWED_ORIGINS`
- 报表下载失败：确认登录权限包含 `report:export`
- MySQL 连接失败：确认云数据库网络（内网/外网）与安全组、账号权限

