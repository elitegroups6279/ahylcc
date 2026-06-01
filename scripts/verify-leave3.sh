#!/bin/bash
# Try different passwords
for PW in "admin123" "hf123456" "Hfnew2024" "123456" "password" "Hfyl,.123456"; do
  RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"admin\",\"password\":\"$PW\"}")
  echo "Password '$PW': $RESP"
  CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))")
  if [ "$CODE" = "200" ]; then
    TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
    echo "SUCCESS! Token: $TOKEN"
    echo "=== June 2026 Calendar Events ==="
    curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/dashboard/calendar-events?year=2026&month=6'
    break
  fi
done