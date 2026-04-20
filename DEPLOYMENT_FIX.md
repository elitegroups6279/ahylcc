# HFNEW项目前端部署指南

## 问题修复说明

### 修复的问题
前端API配置错误，导致生产环境中无法访问后端API。
- **原配置**：`http://localhost:8080` (生产环境无效)
- **新配置**：`/api` (相对路径，通过Nginx反向代理)

## 手动部署步骤

### 方案一：使用部署脚本（推荐）

1. **在本地Windows PowerShell中执行**：
```powershell
cd e:\Hfnew\deploy
bash server-deploy.sh
```

2. **输入服务器密码**：按提示输入ubuntu用户的密码

3. **确认部署**：输入 `yes` 确认部署

### 方案二：手动部署（如果脚本执行失败）

#### 步骤1：连接服务器
```bash
ssh ubuntu@43.138.7.82
```

#### 步骤2：清理旧的前端文件
```bash
sudo rm -rf /opt/hfnew/frontend/*
sudo mkdir -p /opt/hfnew/frontend/assets
```

#### 步骤3：上传新的前端文件

在本地Windows PowerShell中执行（需要输入密码）：
```powershell
cd e:\Hfnew\frontend\dist
scp -r . ubuntu@43.138.7.82:/tmp/frontend-new/
```

#### 步骤4：在服务器上安装新文件

在服务器SSH会话中执行：
```bash
sudo cp -r /tmp/frontend-new/* /opt/hfnew/frontend/
sudo chown -R root:root /opt/hfnew/frontend
sudo chmod -R 755 /opt/hfnew/frontend
```

#### 步骤5：验证部署

检查前端文件：
```bash
ls -la /opt/hfnew/frontend/
```

检查API配置是否正确：
```bash
grep -o '/api' /opt/hfnew/frontend/assets/index-*.js | head -1
```

## 验证修复结果

### 1. 测试后端API
```bash
curl -X POST -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin123!"}' http://localhost:8080/api/auth/login
```

期望返回：包含 `accessToken` 和 `refreshToken` 的JSON响应

### 2. 测试前端访问
在浏览器中访问：`http://43.138.7.82/`

### 3. 测试登录功能
- 用户名：`admin`
- 密码：`Admin123!`

## 检查清单

- [ ] 前端文件已部署到 `/opt/hfnew/frontend/`
- [ ] 新的前端JavaScript文件包含 `/api` 路径
- [ ] 后端服务运行正常：`systemctl status hfnew-backend`
- [ ] Nginx服务运行正常：`systemctl status nginx`
- [ ] 登录功能正常工作
- [ ] 登录后能正常跳转到Dashboard
- [ ] 各页面菜单显示正常

## 故障排查

### 如果登录仍然失败

1. **检查浏览器控制台**：
   - 按F12打开开发者工具
   - 查看Console标签页的错误信息
   - 查看Network标签页，检查API请求状态

2. **检查后端日志**：
```bash
journalctl -u hfnew-backend -f
```

3. **检查Nginx日志**：
```bash
tail -f /var/log/nginx/error.log
```

4. **验证API配置**：
```bash
cat /opt/hfnew/frontend/assets/index-*.js | grep -o 'baseURL.*api'
```

应该看到：`baseURL: "/api"`

## 重要配置信息

- **前端地址**：`http://43.138.7.82/`
- **后端API**：`http://43.138.7.82/api/`
- **数据库**：`hfcc` (腾讯云MySQL)
- **默认管理员**：`admin` / `Admin123!`

## 相关文件位置

- **前端配置**：`e:\Hfnew\frontend\.env.production`
- **前端构建产物**：`e:\Hfnew\frontend\dist\`
- **服务器前端目录**：`/opt/hfnew/frontend/`
- **Nginx配置**：`/etc/nginx/sites-available/hfnew.conf`
- **后端服务**：`/etc/systemd/system/hfnew-backend.service`
