#!/bin/bash

echo 'SPARK_HOME=/usr/hdp/current/spark2-client' >> ~/.bash_profile
echo 'export SPARK_HOME' >> ~/.bash_profile
echo 'HADOOP_HOME=/usr/hdp/current/hadoop-client' >> ~/.bash_profile
echo 'export HADOOP_HOME' >> ~/.bash_profile
echo 'PATH=$PATH:$SPARK_HOME/bin' >> ~/.bash_profile
echo 'export PATH' >> ~/.bash_profile

SPARK_HOME=/usr/hdp/current/spark2-client
export SPARK_HOME
echo $SPARK_HOME

HADOOP_HOME=/usr/hdp/current/hadoop-client
export HADOOP_HOME
echo $HADOOP_HOME




