#!/bin/bash
set -e

echo "=== Step 1: Restore database (one -> hfcc) ==="
mysql -h 172.21.16.6 -u root -p'Hfyl,.123456' -e "DROP DATABASE IF EXISTS hfcc; CREATE DATABASE hfcc CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysqldump --set-gtid-purged=OFF -h 172.21.16.6 -u root -p'Hfyl,.123456' one | mysql -h 172.21.16.6 -u root -p'Hfyl,.123456' hfcc

echo "=== Step 2: Apply schema fixes ==="
mysql -h 172.21.16.6 -u root -p'Hfyl,.123456' hfcc -e "
ALTER TABLE t_bed_transfer MODIFY from_bed_id BIGINT NULL;
"

mysql -h 172.21.16.6 -u root -p'Hfyl,.123456' hfcc -e "
CREATE TABLE IF NOT EXISTS t_elderly_change_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  elderly_id BIGINT NOT NULL,
  field_name VARCHAR(50) NOT NULL,
  old_value VARCHAR(500),
  new_value VARCHAR(500),
  changed_by VARCHAR(100),
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_elderly_id (elderly_id)
);
"

mysql -h 172.21.16.6 -u root -p'Hfyl,.123456' hfcc -e "
UPDATE t_elderly SET disability_level = 'INTACT' WHERE disability_level = 'SELF_CARE';
"

echo "=== Step 3: Deploy backend ==="
sudo cp /tmp/hfnew-backend-1.5.0.jar /opt/hfnew/
sudo sed -i 's/hfnew-backend-[0-9.]*.jar/hfnew-backend-1.5.0.jar/' /etc/systemd/system/hfnew-backend.service
sudo systemctl daemon-reload
sudo systemctl restart hfnew-backend

echo "=== Step 4: Deploy frontend ==="
sudo rm -rf /opt/hfnew/frontend/*
cd /opt/hfnew/frontend
sudo tar -xzf /tmp/dist.tar.gz

echo "=== Deployment complete ==="
