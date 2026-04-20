#!/bin/bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin123!"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
echo "Checking discharged elderly list..."
curl -s "http://localhost:8080/api/elderly?status=DISCHARGED&page=1&pageSize=100" -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json; d=json.load(sys.stdin); print("Total:", d.get("data",{}).get("total",0)); [print(r.get("id"), r.get("name"), r.get("status"), r.get("dischargeDate")) for r in d.get("data",{}).get("list",[])]'
