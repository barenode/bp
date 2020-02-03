ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -copyToLocal /data/books/ratings-train.parquet /opt/dev/target


