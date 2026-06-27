#!/bin/bash
# ========================================
# 自动打包并部署到云服务器 v1.8.0
# ========================================

set -e  # 遇到错误立即退出

echo -e "\033[0;32m========================================\033[0m"
echo -e "\033[0;32m自动打包并部署 v1.8.0\033[0m"
echo -e "\033[0;32m========================================\033[0m\n"

# 配置
SSH_HOST="43.138.7.82"
SSH_USER="ubuntu"
SERVER_PORT="22"
APP_DIR="/opt/hfnew"

# 检查参数
if [ -z "$1" ]; then
    echo -e "\033[0;31m错误: 请提供SSH密码\033[0m"
    echo "用法: ./auto-deploy.sh <ssh-password>"
    exit 1
fi

SSH_PASSWORD="$1"

cd "$(dirname "$0")/.."

# Step 1: 构建后端
echo -e "\n\033[1;33m[1/5] 构建后端...\033[0m"
cd backend
mvn clean package -DskipTests
if [ ! -f "target/hfnew-backend-1.8.0.jar" ]; then
    echo -e "\033[0;31m错误: 后端构建失败\033[0m"
    exit 1
fi
echo -e "\033[0;32m✓ 后端构建成功\033[0m"
cd ..

# Step 2: 构建前端
echo -e "\n\033[1;33m[2/5] 构建前端...\033[0m"
cd frontend
npm run build
if [ ! -d "dist" ]; then
    echo -e "\033[0;31m错误: 前端构建失败\033[0m"
    exit 1
fi
echo -e "\033[0;32m✓ 前端构建成功\033[0m"
cd ..

# Step 3: 上传文件到服务器
echo -e "\n\033[1;33m[3/5] 上传文件到服务器...\033[0m"

# 创建临时目录
sshpass -p "$SSH_PASSWORD" ssh -o StrictHostKeyChecking=no -p $SERVER_PORT ${SSH_USER}@${SSH_HOST} "
    rm -rf /tmp/hfnew_upload
    mkdir -p /tmp/hfnew_upload
    mkdir -p $APP_DIR/backend
    mkdir -p $APP_DIR/frontend
    mkdir -p $APP_DIR/logs
"

# 上传后端JAR
echo "上传后端 JAR..."
sshpass -p "$SSH_PASSWORD" scp -o StrictHostKeyChecking=no -P $SERVER_PORT \
    backend/target/hfnew-backend-1.8.0.jar \
    ${SSH_USER}@${SSH_HOST}:/tmp/hfnew_upload/

# 上传前端dist
echo "上传前端 dist..."
sshpass -p "$SSH_PASSWORD" scp -o StrictHostKeyChecking=no -r -P $SERVER_PORT \
    frontend/dist/* \
    ${SSH_USER}@${SSH_HOST}:/tmp/hfnew_upload/

echo -e "\033[0;32m✓ 文件上传完成\033[0m"

# Step 4: 在服务器上安装
echo -e "\n\033[1;33m[4/5] 在服务器上安装...\033[0m"

sshpass -p "$SSH_PASSWORD" ssh -o StrictHostKeyChecking=no -p $SERVER_PORT ${SSH_USER}@${SSH_HOST} "
    # 停止服务
    sudo systemctl stop hfnew-backend.service 2>/dev/null || true
    
    # 备份旧版本
    if [ -d '$APP_DIR' ]; then
        BACKUP_FILE='$APP_DIR/backup/hfnew_backup_\$(date +%Y%m%d_%H%M%S).tar.gz'
        mkdir -p $APP_DIR/backup
        tar -czf \$BACKUP_FILE -C $APP_DIR . 2>/dev/null && echo '备份完成: '\$BACKUP_FILE || echo '无需备份'
    fi
    
    # 移动文件
    sudo mv /tmp/hfnew_upload/hfnew-backend-1.8.0.jar $APP_DIR/backend/
    sudo cp -r /tmp/hfnew_upload/dist/* $APP_DIR/frontend/
    
    # 设置权限
    sudo chown -R ubuntu:ubuntu $APP_DIR
    sudo chmod +x $APP_DIR/backend/hfnew-backend-1.8.0.jar
    sudo chmod -R 755 $APP_DIR/frontend
    
    # 清理
    rm -rf /tmp/hfnew_upload
    
    echo '安装完成'
"

echo -e "\033[0;32m✓ 安装完成\033[0m"

# Step 5: 重启服务
echo -e "\n\033[1;33m[5/5] 重启服务...\033[0m"

sshpass -p "$SSH_PASSWORD" ssh -o StrictHostKeyChecking=no -p $SERVER_PORT ${SSH_USER}@${SSH_HOST} "
    # 重新加载systemd（如果服务文件有变化）
    sudo systemctl daemon-reload 2>/dev/null || true
    
    # 启动服务
    sudo systemctl start hfnew-backend.service
    
    # 等待3秒
    sleep 3
    
    # 检查服务状态
    if sudo systemctl is-active --quiet hfnew-backend.service; then
        echo '✓ 后端服务已启动'
    else
        echo '✗ 后端服务启动失败'
        sudo systemctl status hfnew-backend.service
        exit 1
    fi
    
    # 重启Nginx
    sudo nginx -t && sudo systemctl reload nginx
    echo '✓ Nginx已重启'
"

echo -e "\n\033[0;32m========================================\033[0m"
echo -e "\033[0;32m✓ 部署完成！\033[0m"
echo -e "\033[0;32m========================================\033[0m"
echo ""
echo "访问地址:"
echo "  前端: http://${SSH_HOST}/"
echo "  后端API: http://${SSH_HOST}/api/"
echo ""
echo "查看日志:"
echo "  后端: ssh ${SSH_USER}@${SSH_HOST} 'journalctl -u hfnew-backend -f'"
echo ""
