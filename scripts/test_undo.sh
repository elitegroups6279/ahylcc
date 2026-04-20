#!/bin/bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin123!"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
echo "Testing undo-discharge..."
curl -s -X PUT "http://localhost:8080/api/elderly/1/undo-discharge" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -w "\nHTTP: %{http_code}\n"
