#!/bin/bash

sudo yum install -y ambari-server

sudo /usr/sbin/ambari-server setup --verbose --silent --java-home /usr/jdk64/jdk1.8.0_60
sudo /bin/systemctl start ambari-server

sudo /usr/bin/wget --spider --tries 10 --retry-connrefused --no-check-certificate http://localhost:8080
sudo /usr/sbin/ambari-server setup --jdbc-db=mysql --jdbc-driver=/var/www/html/common/mysql-connector-java.jar

sudo /usr/bin/curl -v -k -u admin:admin -H "X-Requested-By:ambari" -X POST http://localhost:8080/api/v1/version_definitions -d @/tmp/vagrant/blueprints/vdf.json
sudo /usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://localhost:8080/api/v1/blueprints/blueprint -d @/tmp/vagrant/blueprints/blueprint-multinode.json
sudo /usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://localhost:8080/api/v1/clusters/cluster -d @/tmp/vagrant/blueprints/cluster-multinode.json