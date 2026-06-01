#!/bin/bash
# Login with correct password Admin123!
RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}')
echo "Login: $RESP"

TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo "Token: $TOKEN"

echo "=== June 2026 Calendar Events ==="
curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/dashboard/calendar-events?year=2026&month=6'