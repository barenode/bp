#!/bin/bash

sudo yum install -y ambari-server

sudo /usr/sbin/ambari-server setup --verbose --silent --java-home /usr/jdk64/jdk1.8.0_231
sudo /bin/systemctl start ambari-server

sudo /usr/bin/wget --spider --tries 10 --retry-connrefused --no-check-certificate http://localhost:8080
sudo /usr/sbin/ambari-server setup --jdbc-db=mysql --jdbc-driver=/var/www/html/common/mysql-connector-java.jar

