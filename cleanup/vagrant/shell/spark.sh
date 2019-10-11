
#!/bin/bash

#
# prerequisities
#
sudo yum install -y mc
sudo yum install -y wget

#
# install JDK 1.8
#
sudo yum install -y java-1.8.0-openjdk

#
# install Spark
#
cd /tmp
sudo /bin/wget https://archive.apache.org/dist/spark/spark-2.3.1/spark-2.3.1-bin-hadoop2.7.tgz
sudo /bin/tar xf spark-2.3.1-bin-hadoop2.7.tgz
sudo /bin/mkdir /usr/local/spark
sudo /bin/cp -r spark-2.3.1-bin-hadoop2.7/* /usr/local/spark
PATH=$PATH:$HOME/bin:/usr/local/spark/bin

echo 'SPARK_HOME=/usr/local/spark' >> ~/.bash_profile
echo 'export SPARK_HOME' >> ~/.bash_profile
echo 'HADOOP_HOME=/usr/local/spark' >> ~/.bash_profile
echo 'export HADOOP_HOME' >> ~/.bash_profile
echo 'PATH=$PATH:$SPARK_HOME/bin' >> ~/.bash_profile
echo 'export PATH' >> ~/.bash_profile

SPARK_HOME=/usr/local/spark
export SPARK_HOME
echo $SPARK_HOME

HADOOP_HOME=$SPARK_HOME
export HADOOP_HOME
echo $HADOOP_HOME

