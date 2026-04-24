#!/bin/bash
TOKEN=$(curl -s -X POST http://localhost/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"wx2291196"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))")
echo "TOKEN=${TOKEN:0:20}..."

# Test duplicate username "hudan" - should return 400
RESP1=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost/api/system/accounts -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d '{"username":"hudan","password":"Test123456","realName":"胡丹","roleIds":[4],"orgId":1}')
echo "DUPLICATE: $RESP1"

# Test create new user with org - should return 200
RESP2=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost/api/system/accounts -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d '{"username":"testuser39v2","password":"Test123456","realName":"测试39v2","roleIds":[4],"orgId":1}')
echo "CREATE: $RESP2"

# Clean up
TESTID=$(echo "$RESP2" | grep -v HTTP_CODE | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',''))")
if [ -n "$TESTID" ] && [ "$TESTID" != "None" ] && [ "$TESTID" != "" ]; then
  curl -s -X DELETE "http://localhost/api/system/accounts/$TESTID" -H "Authorization: Bearer $TOKEN" > /dev/null
  echo "CLEANUP_OK id=$TESTID"
fi

echo "DONE"
