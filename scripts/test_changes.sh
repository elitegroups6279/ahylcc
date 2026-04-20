#!/bin/bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin123!"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
echo "Checking change logs for elderly id=1..."
curl -s "http://localhost:8080/api/elderly/1/changes" -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json; d=json.load(sys.stdin); [print(c.get("fieldLabel",""), c.get("oldValue",""), "->", c.get("newValue","")) for c in d.get("data",[])[:10]]'
