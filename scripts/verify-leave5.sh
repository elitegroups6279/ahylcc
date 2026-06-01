#!/bin/bash
# Reset admin password to Admin123! using the known bcrypt hash from hfnew_full.sql
mysql -u root -p'Hfyl,.123456' -h 172.21.16.6 hfcc -e "UPDATE t_user SET password='\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' WHERE username='admin';"

# Now login
RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}')
echo "Login: $RESP"

TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

echo "=== June 2026 Calendar Events ==="
curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/dashboard/calendar-events?year=2026&month=6'