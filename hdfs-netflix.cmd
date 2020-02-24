#ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -mkdir /data"
#ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/netflix_training_set    /data/netflix_training_set"
ssh -i %HOMEDRIVE%%HOMEPATH%/.ssh/vagrant_private_key vagrant@192.168.72.101 "hdfs dfs -copyFromLocal /opt/dev/src/main/jupyter/data/lastfm-dataset-360K    /data/lastfm-dataset-360K"



