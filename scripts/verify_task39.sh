#!/bin/bash
set -e
TOKEN=$(curl -s -X POST http://localhost/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"wx2291196"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))")
echo "TOKEN=${TOKEN:0:20}..."

# Test 1: Duplicate username should return 400, not 500
RESP1=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost/api/system/accounts -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d '{"username":"hudan","password":"Test123456","realName":"胡丹","roleIds":[4],"orgId":1}')
echo "DUPLICATE_TEST: $RESP1"

# Test 2: Create new user should succeed
RESP2=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost/api/system/accounts -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d '{"username":"testuser500fix","password":"Test123456","realName":"测试500修复","roleIds":[4],"orgId":1}')
echo "CREATE_TEST: $RESP2"

# Clean up test user
TESTID=$(echo "$RESP2" | head -1 | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',''))")
if [ -n "$TESTID" ] && [ "$TESTID" != "None" ] && [ "$TESTID" != "" ]; then
  curl -s -X DELETE "http://localhost/api/system/accounts/$TESTID" -H "Authorization: Bearer $TOKEN" > /dev/null || true
  echo "CLEANUP: deleted testuser500fix (id=$TESTID)"
fi

echo "VERIFICATION_COMPLETE"
