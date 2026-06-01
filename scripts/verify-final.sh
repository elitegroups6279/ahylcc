#!/bin/bash
# Generate bcrypt hash for Admin123! using python
HASH=$(python3 -c "
import bcrypt
hashed = bcrypt.hashpw('Admin123!'.encode('utf-8'), bcrypt.gensalt(10))
print(hashed.decode('utf-8'))
")
echo "Generated hash: $HASH"

# Update admin password in DB
mysql -u root -p'Hfyl,.123456' -h 172.21.16.6 hfcc <<EOF
UPDATE t_user SET password='$HASH' WHERE username='admin';
SELECT id, username, LEFT(password, 40) as pwd FROM t_user WHERE username='admin';
EOF

# Now login
sleep 1
RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}')
echo "Login: $RESP"

TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

echo "=== June 2026 Calendar Events ==="
curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/dashboard/calendar-events?year=2026&month=6'