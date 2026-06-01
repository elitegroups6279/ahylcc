#!/bin/bash
# First check login response
LOGIN_RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
echo "Login response: $LOGIN_RESP"

# Extract token
TOKEN=$(echo "$LOGIN_RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token","NO_TOKEN") if d.get("data") else "NO_DATA")')
echo "Token: $TOKEN"

if [ "$TOKEN" != "NO_TOKEN" ] && [ "$TOKEN" != "NO_DATA" ]; then
  echo "=== Calendar Events for June 2026 ==="
  curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/dashboard/calendar-events?year=2026&month=6'
fi