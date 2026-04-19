#!/bin/bash
# ========================================
# 养老企业管理系统 v1.0.0 服务器部署脚本
# 适用于: Ubuntu Server 24.04 LTS
# ========================================

# 配置变量
APP_NAME="hfnew"
APP_DIR="/opt/${APP_NAME}"
BACKUP_DIR="/opt/${APP_NAME}/backup"
DEPLOY_USER="root"
SSH_USER="root"
SSH_HOST="43.138.7.82"  # 请修改为您的服务器IP
SERVER_PORT="22"

# 远程部署路径
REMOTE_APP_DIR="/opt/${APP_NAME}"
REMOTE_UPLOAD_DIR="/tmp/${APP_NAME}_upload"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}养老企业管理系统 v1.0.0 部署脚本${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查本地文件
echo ""
echo -e "${YELLOW}检查本地构建产物...${NC}"

# 检查后端JAR
if [ ! -f "../backend/target/hfnew-backend-1.0.0.jar" ]; then
    echo -e "${RED}错误: 后端JAR文件不存在${NC}"
    echo -e "${RED}请先运行: cd backend && mvn clean package -DskipTests${NC}"
    exit 1
fi

# 检查前端dist
if [ ! -d "../frontend/dist" ]; then
    echo -e "${RED}错误: 前端构建产物不存在${NC}"
    echo -e "${RED}请先运行: cd frontend && npm run build${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 本地文件检查通过${NC}"

# 询问确认
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}部署配置:${NC}"
echo -e "${YELLOW}========================================${NC}"
echo "服务器IP: ${SSH_HOST}"
echo "应用目录: ${REMOTE_APP_DIR}"
echo "后端JAR: hfnew-backend-1.0.0.jar"
echo "前端目录: dist/"
echo ""
read -p "确认部署? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "部署已取消"
    exit 0
fi

# 执行远程部署
echo ""
echo -e "${GREEN}开始远程部署...${NC}"
echo ""

# 构建SSH命令
SSH_CMD="ssh -p ${SERVER_PORT} ${SSH_USER}@${SSH_HOST}"

# 远程命令序列
REMOTE_SCRIPT="
# 1. 停止旧版本服务
echo -e '${YELLOW}停止旧版本服务...${NC}'
systemctl stop hfnew-backend.service 2>/dev/null || echo '服务未运行或不存在'

# 2. 备份旧版本（如果存在）
echo -e '${YELLOW}备份旧版本...${NC}'
if [ -d '${REMOTE_APP_DIR}' ]; then
    BACKUP_FILE='${BACKUP_DIR}/hfnew_backup_\$(date +%Y%m%d_%H%M%S).tar.gz'
    mkdir -p '${BACKUP_DIR}'
    tar -czf \${BACKUP_FILE} -C '${REMOTE_APP_DIR}' . 2>/dev/null && echo '备份完成: '\${BACKUP_FILE} || echo '无需备份'
fi

# 3. 清除旧版本
echo -e '${YELLOW}清除旧版本...${NC}'
rm -rf '${REMOTE_APP_DIR}' && mkdir -p '${REMOTE_APP_DIR}' && echo '旧版本已清除' || echo '清除失败'

# 4. 创建必要目录
echo -e '${YELLOW}创建应用目录...${NC}'
mkdir -p '${REMOTE_APP_DIR}/backend'
mkdir -p '${REMOTE_APP_DIR}/frontend'
mkdir -p '${REMOTE_APP_DIR}/logs'
mkdir -p '${REMOTE_APP_DIR}/backup'

# 5. 清空上传目录
rm -rf '${REMOTE_UPLOAD_DIR}' && mkdir -p '${REMOTE_UPLOAD_DIR}'

echo '远程准备完成'
"

# 执行远程准备命令
echo -e "${YELLOW}执行服务器准备操作...${NC}"
echo "$REMOTE_SCRIPT" | $SSH_CMD

if [ $? -ne 0 ]; then
    echo -e "${RED}服务器准备失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 服务器准备完成${NC}"

# 上传新版本
echo ""
echo -e "${YELLOW}上传新版本文件...${NC}"

# 上传后端JAR
echo "上传后端 JAR..."
scp -P ${SERVER_PORT} "../backend/target/hfnew-backend-1.0.0.jar" ${SSH_USER}@${SSH_HOST}:${REMOTE_UPLOAD_DIR}/

if [ $? -ne 0 ]; then
    echo -e "${RED}后端JAR上传失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 后端JAR上传完成${NC}"

# 上传前端dist
echo "上传前端 dist..."
scp -r -P ${SERVER_PORT} "../frontend/dist/" ${SSH_USER}@${SSH_HOST}:${REMOTE_UPLOAD_DIR}/

if [ $? -ne 0 ]; then
    echo -e "${RED}前端dist上传失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 前端dist上传完成${NC}"

# 远程安装
echo ""
echo -e "${YELLOW}远程安装新版本...${NC}"

INSTALL_SCRIPT="
# 移动后端文件
echo '安装后端...'
mv '${REMOTE_UPLOAD_DIR}/hfnew-backend-1.0.0.jar' '${REMOTE_APP_DIR}/backend/'

# 移动前端文件
echo '安装前端...'
cp -r '${REMOTE_UPLOAD_DIR}/dist/*' '${REMOTE_APP_DIR}/frontend/'

# 设置权限
echo '设置文件权限...'
chown -R root:root '${REMOTE_APP_DIR}'
chmod +x '${REMOTE_APP_DIR}/backend/hfnew-backend-1.0.0.jar'
chmod -R 755 '${REMOTE_APP_DIR}/frontend'

# 清理上传目录
rm -rf '${REMOTE_UPLOAD_DIR}'

echo '安装完成'
"

echo "$INSTALL_SCRIPT" | $SSH_CMD

if [ $? -ne 0 ]; then
    echo -e "${RED}安装失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 安装完成${NC}"

# 重启Nginx
echo ""
echo -e "${YELLOW}重启Nginx...${NC}"

NGINX_RESTART="nginx -t && systemctl reload nginx"

echo "$NGINX_RESTART" | $SSH_CMD

if [ $? -ne 0 ]; then
    echo -e "${RED}Nginx重启失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Nginx重启完成${NC}"

# 启动后端服务
echo ""
echo -e "${YELLOW}启动后端服务...${NC}"

START_SCRIPT="
# 创建systemd服务文件（如果不存在）
if [ ! -f '/etc/systemd/system/hfnew-backend.service' ]; then
    cat > /etc/systemd/system/hfnew-backend.service << 'EOF'
[Unit]
Description=Hfnew Backend Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${REMOTE_APP_DIR}/backend
ExecStart=/usr/bin/java -jar ${REMOTE_APP_DIR}/backend/hfnew-backend-1.0.0.jar
Restart=on-failure
RestartSec=10

EnvironmentFile=${REMOTE_APP_DIR}/backend.env

[Install]
WantedBy=multi-user.target
EOF
    
    # 重新加载systemd
    systemctl daemon-reload
    
    echo 'systemd服务文件已创建'
fi

# 创建环境变量文件（如果不存在）
if [ ! -f '${REMOTE_APP_DIR}/backend.env' ]; then
    cat > '${REMOTE_APP_DIR}/backend.env' << 'EOF'
# 数据库配置
MYSQL_HOST=j-cynosdbmysql-grp-q7xgsuf2.sql.tencentcdb.com
MYSQL_PORT=21671
MYSQL_DATABASE=hfcc
MYSQL_USERNAME=root
MYSQL_PASSWORD=

# JWT配置
APP_JWT_ACCESS_SECRET=CHANGE_ME_ACCESS_SECRET_CHANGE_ME_ACCESS_SECRET
APP_JWT_REFRESH_SECRET=CHANGE_ME_REFRESH_SECRET_CHANGE_ME_REFRESH_SECRET
APP_JWT_ACCESS_TTL_SECONDS=900
APP_JWT_REFRESH_TTL_SECONDS=1209600
APP_JWT_ISSUER=hfnew-backend

# 安全配置
APP_SECURITY_ALLOWED_ORIGINS=http://www.ahylcc.cn,https://www.ahylcc.cn
EOF
    
    echo '环境变量文件已创建，请修改数据库密码和JWT密钥'
fi

# 启动服务
systemctl start hfnew-backend.service

# 检查服务状态
sleep 3
if systemctl is-active --quiet hfnew-backend.service; then
    echo '后端服务已启动'
else
    echo '后端服务启动失败，查看日志: journalctl -u hfnew-backend -f'
    systemctl status hfnew-backend.service
fi

# 显示服务状态
systemctl status hfnew-backend.service
"

echo "$START_SCRIPT" | $SSH_CMD

if [ $? -ne 0 ]; then
    echo -e "${RED}后端服务启动失败${NC}"
    echo "请手动检查日志: ssh ${SSH_USER}@${SSH_HOST} 'journalctl -u hfnew-backend -f'"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ 部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "服务访问地址:"
echo "  前端: http://${SSH_HOST}/"
echo "  后端API: http://${SSH_HOST}/api/"
echo ""
echo "默认管理员账号:"
echo "  用户名: admin"
echo "  密码: Admin123!"
echo ""
echo "注意事项:"
echo "  1. 请登录服务器修改 ${REMOTE_APP_DIR}/backend.env 中的数据库密码和JWT密钥"
echo "  2. 数据库密码修改后，重启后端服务: systemctl restart hfnew-backend.service"
echo "  3. 数据库连接配置在: ${REMOTE_APP_DIR}/backend/hfnew-backend-1.0.0.jar 的 application.yml 中"
echo ""
echo "查看服务状态:"
echo "  后端: ssh ${SSH_USER}@${SSH_HOST} 'systemctl status hfnew-backend.service'"
echo "  后端日志: ssh ${SSH_USER}@${SSH_HOST} 'journalctl -u hfnew-backend -f'"
echo "  Nginx日志: ssh ${SSH_USER}@${SSH_HOST} 'tail -f /var/log/nginx/access.log'"
echo ""
