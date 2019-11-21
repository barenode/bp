ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -mkdir /data"
ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -mkdir /data/books"
ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/BX-Book-Ratings.csv /data/books"
ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/BX-Books.csv /data/books"
ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/BX-Users.csv /data/books"


