sudo su --command 'hdfs dfs -mkdir /user/spark/donation' spark 
sudo su --command hdfs dfs -put /opt/dataset/donation/block_*.csv /user/spark/donation  spark