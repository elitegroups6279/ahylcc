# ========================================
# 自动打包并部署到云服务器 v1.8.0 (PowerShell)
# ========================================

param(
    [Parameter(Mandatory=$true)]
    [string]$SshPassword
)

$ErrorActionPreference = "Stop"

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "自动打包并部署 v1.8.0" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

# 配置
$sshHost = "43.138.7.82"
$sshUser = "ubuntu"
$serverPort = "22"
$appDir = "/opt/hfnew"
$projectRoot = Split-Path $PSScriptRoot -Parent

Set-Location $projectRoot

# Step 1: 构建后端
Write-Host "`n[1/5] 构建后端..." -ForegroundColor Yellow
Set-Location "backend"
mvn clean package -DskipTests
if (-not (Test-Path "target\hfnew-backend-1.8.0.jar")) {
    Write-Host "错误: 后端构建失败" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 后端构建成功" -ForegroundColor Green
Set-Location ..

# Step 2: 构建前端
Write-Host "`n[2/5] 构建前端..." -ForegroundColor Yellow
Set-Location "frontend"
npm run build
if (-not (Test-Path "dist")) {
    Write-Host "错误: 前端构建失败" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 前端构建成功" -ForegroundColor Green
Set-Location ..

# Step 3: 上传文件到服务器
Write-Host "`n[3/5] 上传文件到服务器..." -ForegroundColor Yellow

# 使用PSCP或scp上传
Write-Host "上传后端 JAR..." -ForegroundColor Cyan
$backendJar = "backend\target\hfnew-backend-1.8.0.jar"
& scp -P $serverPort -o StrictHostKeyChecking=no $backendJar "${sshUser}@${sshHost}:/tmp/"

Write-Host "压缩前端 dist..." -ForegroundColor Cyan
Compress-Archive -Path "frontend\dist\*" -DestinationPath "frontend\dist.zip" -Force

Write-Host "上传前端 dist..." -ForegroundColor Cyan
& scp -P $serverPort -o StrictHostKeyChecking=no "frontend\dist.zip" "${sshUser}@${sshHost}:/tmp/"

Remove-Item "frontend\dist.zip" -Force

Write-Host "✓ 文件上传完成" -ForegroundColor Green

# Step 4 & 5: 在服务器上安装和重启
Write-Host "`n[4/5] 在服务器上安装..." -ForegroundColor Yellow
Write-Host "[5/5] 重启服务..." -ForegroundColor Yellow

$installScript = @"
# 停止服务
systemctl stop hfnew-backend.service 2>/dev/null || true

# 备份旧版本
if [ -d '$appDir' ]; then
    BACKUP_FILE='$appDir/backup/hfnew_backup_\$(date +%Y%m%d_%H%M%S).tar.gz'
    mkdir -p $appDir/backup
    tar -czf \$BACKUP_FILE -C $appDir . 2>/dev/null && echo '备份完成: '\$BACKUP_FILE || echo '无需备份'
fi

# 解压前端
mkdir -p /tmp/dist_extracted
unzip -o /tmp/dist.zip -d /tmp/dist_extracted

# 移动文件
mv /tmp/hfnew-backend-1.8.0.jar $appDir/backend/
rm -rf $appDir/frontend/*
cp -r /tmp/dist_extracted/* $appDir/frontend/

# 设置权限
chown -R root:root $appDir
chmod +x $appDir/backend/hfnew-backend-1.8.0.jar
chmod -R 755 $appDir/frontend

# 清理
rm -rf /tmp/dist_extracted /tmp/dist.zip

echo '安装完成'

# 启动服务
systemctl daemon-reload 2>/dev/null || true
systemctl start hfnew-backend.service
sleep 3

if systemctl is-active --quiet hfnew-backend.service; then
    echo '✓ 后端服务已启动'
else
    echo '✗ 后端服务启动失败'
    systemctl status hfnew-backend.service
    exit 1
fi

# 重启Nginx
nginx -t && systemctl reload nginx
echo '✓ Nginx已重启'
"@

# 通过SSH执行远程脚本
$encodedScript = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($installScript))
$remoteCmd = "echo '$encodedScript' | base64 -d | bash"

Write-Host "执行远程部署脚本..." -ForegroundColor Cyan
& ssh -p $serverPort -o StrictHostKeyChecking=no ${sshUser}@${sshHost} $remoteCmd

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "✓ 部署完成！" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green
Write-Host "访问地址:"
Write-Host "  前端: http://${sshHost}/"
Write-Host "  后端API: http://${sshHost}/api/"
Write-Host "`n查看日志:"
Write-Host "  后端: ssh ${sshUser}@${sshHost} 'journalctl -u hfnew-backend -f'"
Write-Host ""
