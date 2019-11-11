#!/bin/bash

sudo /usr/bin/curl -v -k -u admin:admin -H "X-Requested-By:ambari" -X POST http://localhost:8080/api/v1/version_definitions -d @/tmp/vagrant/blueprints/vdf.json
sudo /usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://localhost:8080/api/v1/blueprints/blueprint -d @/tmp/vagrant/blueprints/blueprint-multinode.json
sudo /usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://localhost:8080/api/v1/clusters/cluster -d @/tmp/vagrant/blueprints/cluster-multinode.json