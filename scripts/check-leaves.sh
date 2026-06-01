#!/bin/bash
# Check leave records in the database
mysql -u root -p'Hfyl,.123456' -h 172.21.16.6 hfcc -e "SELECT id, elderly_id, status, start_date, end_date, return_date FROM t_elderly_leave WHERE deleted=0 AND status IN ('ON_LEAVE','RETURNED');"

# Also verify the password hash was updated correctly
mysql -u root -p'Hfyl,.123456' -h 172.21.16.6 hfcc -e "SELECT id, username, LEFT(password, 40) as pwd FROM t_user WHERE username='admin';"