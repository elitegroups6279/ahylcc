sudo cp -r /tmp/frontend-new/* /opt/hfnew/frontend/
sudo chown -R root:root /opt/hfnew/frontend
sudo chmod -R 755 /opt/hfnew/frontend
ls -la /opt/hfnew/frontend/
echo "---"
grep -o '/api' /opt/hfnew/frontend/assets/index-*.js | head -1
