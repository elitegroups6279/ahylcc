# HFNEW前端快速修复部署脚本

## 问题说明
服务器上当前的前端文件不完整，缺少关键的index.html和JavaScript文件，导致登录功能无法正常工作。

## 修复内容
1. ✅ 创建了正确的生产环境配置文件 `.env.production`
2. ✅ 使用正确的API配置 (`/api`) 重新构建了前端
3. ✅ 验证了新构建产物包含正确的API路径

## 自动化部署步骤

### 方法一：一键部署（推荐）

在PowerShell中运行以下命令：

```powershell
# 进入项目目录
cd e:\Hfnew

# 创建临时部署脚本
@"
#!/bin/bash
# 清理服务器旧文件
echo "清理服务器旧文件..."
ssh -o StrictHostKeyChecking=no ubuntu@43.138.7.82 "sudo rm -rf /opt/hfnew/frontend/* && sudo mkdir -p /opt/hfnew/frontend/assets"

# 上传新构建的前端文件
echo "上传前端文件..."
scp -r -o StrictHostKeyChecking=no frontend/dist/* ubuntu@43.138.7.82:/tmp/frontend-new/

# 安装新文件
echo "安装新文件..."
ssh -o StrictHostKeyChecking=no ubuntu@43.138.7.82 "sudo cp -r /tmp/frontend-new/* /opt/hfnew/frontend/ && sudo chown -R root:root /opt/hfnew/frontend && sudo chmod -R 755 /opt/hfnew/frontend"

# 验证部署
echo "验证部署..."
ssh -o StrictHostKeyChecking=no ubuntu@43.138.7.82 "ls -la /opt/hfnew/frontend/ && echo '---' && grep -o '/api' /opt/hfnew/frontend/assets/index-*.js | head -1"

echo "部署完成！"
"@ | Out-File -FilePath deploy-frontend.ps1 -Encoding utf8

# 执行部署
powershell -ExecutionPolicy Bypass -File deploy-frontend.ps1
```

### 方法二：手动逐步部署

如果一键部署失败，请按以下步骤手动执行：

#### 步骤1：清理服务器旧文件
```powershell
ssh ubuntu@43.138.7.82 "sudo rm -rf /opt/hfnew/frontend/* && sudo mkdir -p /opt/hfnew/frontend/assets"
```

#### 步骤2：上传前端文件到临时目录
```powershell
cd e:\Hfnew\frontend\dist
scp -r . ubuntu@43.138.7.82:/tmp/frontend-new/
```

#### 步骤3：安装到生产目录
```powershell
ssh ubuntu@43.138.7.82 "sudo cp -r /tmp/frontend-new/* /opt/hfnew/frontend/ && sudo chown -R root:root /opt/hfnew/frontend && sudo chmod -R 755 /opt/hfnew/frontend"
```

#### 步骤4：验证部署
```powershell
ssh ubuntu@43.138.7.82 "ls -la /opt/hfnew/frontend/ && echo '---' && grep -o '/api' /opt/hfnew/frontend/assets/index-*.js | head -1"
```

## 验证修复结果

### 1. 检查前端文件完整性
访问：`http://43.138.7.82/`
应该能看到登录页面

### 2. 测试登录功能
- 用户名：`admin`
- 密码：`Admin123!`

### 3. 检查浏览器控制台
按F12打开开发者工具，查看：
- Console标签页：不应该有"Network Error"
- Network标签页：API请求应该指向 `/api/...` 而不是 `http://localhost:8080/...`

### 4. 验证API配置
在服务器上运行：
```bash
grep -o 'baseURL.*api' /opt/hfnew/frontend/assets/index-*.js | head -1
```

应该看到：`baseURL: "/api"`

## 故障排查

### 如果部署后仍然有错误：

1. **清除浏览器缓存**
   - 按Ctrl+Shift+Delete清除缓存
   - 或按Ctrl+F5强制刷新

2. **检查Nginx配置**
   ```bash
   sudo nginx -t
   sudo systemctl reload nginx
   ```

3. **检查后端服务**
   ```bash
   sudo systemctl status hfnew-backend
   sudo journalctl -u hfnew-backend -f
   ```

4. **检查前端文件**
   ```bash
   ls -la /opt/hfnew/frontend/
   cat /opt/hfnew/frontend/index.html
   ```

## 关键信息

- **前端地址**：`http://43.138.7.82/`
- **后端API**：`http://43.138.7.82/api/`
- **正确的API路径**：`/api` (相对路径)
- **错误的API路径**：`http://localhost:8080` (绝对路径)

## 技术说明

修复的核心问题是：
- **之前**：前端硬编码了 `http://localhost:8080` 作为API地址
- **现在**：使用 `/api` 作为相对路径，由Nginx反向代理到后端

这样在前端生产环境中，API请求会正确地通过Nginx路由到后端服务。
