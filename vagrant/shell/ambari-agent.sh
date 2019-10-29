#!/bin/bash

#disable selinux temporaly
sudo setenforce 0

sudo /bin/mkdir /usr/jdk64
sudo /bin/cd /usr/jdk64
sudo /bin/cp -n /var/www/html/common/jdk-8u60-linux-x64.tar.gz /usr/jdk64/jdk-8u60-linux-x64.tar.gz
sudo /bin/gunzip -c /usr/jdk64/jdk-8u60-linux-x64.tar.gz > /usr/jdk64/jdk-8u60-linux-x64.tar
sudo /bin/tar xf /usr/jdk64/jdk-8u60-linux-x64.tar -C /usr/jdk64

sudo /bin/cp -f /tmp/vagrant/config/hosts /etc/hosts
sudo yum install -y mc
sudo yum install -y wget
sudo /bin/systemctl stop firewalld
sudo yum install -y ntp
sudo /bin/systemctl start ntpd
sudo yum install -y httpd      
sudo yum install -y yum-plugin-priorities   
sudo /bin/systemctl start httpd
sudo /bin/cp -n /tmp/vagrant/config/AMBARI.repo /etc/yum.repos.d/AMBARI.repo
sudo /bin/cp -n /tmp/vagrant/config/HDP.repo /etc/yum.repos.d/HDP.repo
sudo /bin/cp -n /tmp/vagrant/config/HDP-UTILS.repo /etc/yum.repos.d/HDP-UTILS.repo

#disable selinux permanently
sudo /bin/sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/sysconfig/selinux

sudo yum install -y ambari-agent
sudo /bin/sed -i 's/hostname=localhost/hostname=c7201.barenode.org/g' /etc/ambari-agent/conf/ambari-agent.ini
sudo /bin/systemctl start ambari-agent

sudo groupadd hdfs
sudo groupadd hadoop
sudo usermod -a -G hdfs vagrant
sudo usermod -a -G hadoop vagrant