# 自动部署指南 v1.8.0

## 概述

本项目提供了自动打包并部署到云服务器的脚本，支持Windows PowerShell和Linux/macOS Bash。

## 前置条件

### Windows
1. 安装 Git for Windows（包含 ssh 和 scp 命令）
2. 安装 Node.js 和 npm
3. 安装 Maven
4. 安装 Java JDK 17+

### Linux/macOS
1. 安装 ssh 和 scp
2. 安装 sshpass（可选，用于密码认证）
   ```bash
   # Ubuntu/Debian
   sudo apt-get install sshpass
   
   # CentOS/RHEL
   sudo yum install sshpass
   ```
3. 安装 Node.js 和 npm
4. 安装 Maven
5. 安装 Java JDK 17+

## 云服务器信息

- **服务器IP**: 43.138.7.82
- **SSH用户**: root
- **SSH端口**: 22
- **应用目录**: /opt/hfnew
- **数据库主机**: 172.21.16.6 (内网)
- **数据库名称**: hfcc
- **数据库用户**: root

## 使用方法

### Windows (PowerShell)

```powershell
cd d:\Hfnew\Hfnew\deploy
.\auto-deploy.ps1 -SshPassword "你的SSH密码"
```

### Linux/macOS (Bash)

```bash
cd /path/to/Hfnew/Hfnew/deploy
chmod +x auto-deploy.sh
./auto-deploy.sh "你的SSH密码"
```

## 部署流程

1. **构建后端** - 使用 Maven 打包 Spring Boot 应用
2. **构建前端** - 使用 npm 构建 Vue 应用
3. **上传文件** - 通过 SCP 上传 JAR 和前端文件
4. **服务器安装** - 停止服务、备份、替换文件
5. **重启服务** - 启动后端服务和 Nginx

## 手动部署（备选方案）

如果自动部署失败，可以使用手动部署脚本：

```bash
cd d:\Hfnew\Hfnew\deploy
chmod +x server-deploy.sh
./server-deploy.sh
```

## 查询数据库

### 查询叶方明请假记录

```bash
cd d:\Hfnew\Hfnew\.tools
node query-ye-leave.js "你的SSH密码"
```

这将查询：
1. 叶方明的基本信息
2. 叶方明的所有请假记录
3. 2024年3月的所有请假记录

### 直接SQL查询

也可以直接使用 SQL 文件：

```sql
-- 查询叶方明的请假记录
SELECT 
    e.name AS elderly_name,
    el.start_date,
    el.end_date,
    el.return_date,
    el.status,
    el.reason
FROM t_elderly_leave el
JOIN t_elderly e ON e.id = el.elderly_id
WHERE e.name LIKE '%叶方明%' 
  AND el.deleted = 0
ORDER BY el.start_date DESC;
```

## 故障排查

### 1. SSH连接失败
- 检查网络连接
- 确认SSH密码正确
- 检查防火墙设置

### 2. 构建失败
- 检查 Maven 和 Node.js 是否正确安装
- 查看构建日志中的错误信息
- 确保依赖已正确下载

### 3. 服务启动失败
```bash
# 查看后端日志
ssh root@43.138.7.82 'journalctl -u hfnew-backend -f'

# 查看Nginx日志
ssh root@43.138.7.82 'tail -f /var/log/nginx/error.log'
```

### 4. 数据库连接问题
- 确认数据库服务正常运行
- 检查数据库密码是否正确
- 验证网络连接（特别是内网访问）

## 安全注意事项

⚠️ **重要提示**:
1. 不要将 SSH 密码提交到版本控制系统
2. 建议使用 SSH 密钥认证替代密码
3. 定期更新数据库密码
4. 生产环境的 JWT 密钥必须修改为强随机字符串

## 备份策略

每次部署前会自动备份到 `/opt/hfnew/backup/` 目录，备份文件名格式：
```
hfnew_backup_YYYYMMDD_HHMMSS.tar.gz
```

如需手动恢复：
```bash
ssh root@43.138.7.82
cd /opt/hfnew/backup
tar -xzf hfnew_backup_YYYYMMDD_HHMMSS.tar.gz -C /opt/hfnew/
systemctl restart hfnew-backend.service
```

## 联系支持

如有问题，请联系系统管理员或查看项目文档。
