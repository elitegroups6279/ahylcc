# e-Hfnew

## P0：前后端联调基线（JWT + Refresh + RBAC）

本阶段目标：从空目录搭建 `backend`（Spring Boot）与 `frontend`（Vue/Vite），完成以下端到端链路：

1. `POST /api/auth/login` 登录下发 `accessToken + refreshToken`
2. `GET /api/rbac/permissions` 返回 `permissions: string[]`（`perm_only`）
3. 前端路由守卫：根据 `permissions` 放行/拦截
4. `GET /api/demo/secure` 受权限保护接口（需要 `demo:secure`）
5. Access 过期后前端自动调用 `POST /api/auth/refresh` 并重试请求

## 测试账号（后端内存假数据）
- `admin` / `Admin123!`（拥有 `demo:secure`）
- `staff` / `Staff123!`（无该权限）

## 后端
目录：`backend/`

前置依赖：
- JDK 17
- Maven（3.8+）

1. 配置 `backend/src/main/resources/application.yml` 中的 `app.jwt.access-secret` / `app.jwt.refresh-secret`
2. 数据库（默认 MySQL，支持 H2）
   - MySQL：通过环境变量配置 `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` / `MYSQL_USERNAME` / `MYSQL_PASSWORD`
   - H2：启用 `h2` profile（例如 `-Dspring.profiles.active=h2`）
3. 启动后端（示例命令，取决于你本机 Maven/IDE 环境）
   - `mvn spring-boot:run`
4. 后端默认端口：`http://localhost:8080`

## 前端
目录：`frontend/`

前置依赖：
- Node.js（建议 18+，包含 npm）

1. 安装依赖：`npm install`
2. 启动开发：`npm run dev`
3. 前端默认地址：`http://localhost:5173`
4. 若需要修改接口地址：设置环境变量 `VITE_API_BASE_URL`

