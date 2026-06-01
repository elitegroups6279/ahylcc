#!/bin/bash
# Query users from database
mysql -u root -p'Hfyl,.123456' -h 172.21.16.6 hfcc -e "SELECT id, username, LEFT(password, 30) as pwd FROM t_user LIMIT 5;"