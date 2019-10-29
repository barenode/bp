hdfs dfs -mkdir /data
hdfs dfs -mkdir /data/books
hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/BX-Book-Ratings.csv /data/books
hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/BX-Books.csv /data/books
hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/BX-Users.csv /data/books


