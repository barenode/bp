
#!/bin/bash

#
# install python3
#
sudo yum install -y centos-release-scl
sudo yum install -y rh-python36
sudo yum groupinstall -y 'Development Tools'

sudo /opt/rh/rh-python36/root/bin/python3 -m pip install --upgrade pip
sudo /opt/rh/rh-python36/root/bin/python3 -m pip install jupyter
sudo /opt/rh/rh-python36/root/bin/python3 -m pip install numpy
sudo /opt/rh/rh-python36/root/bin/python3 -m pip install pandas
sudo /opt/rh/rh-python36/root/bin/python3 -m pip install matplotlib
sudo /opt/rh/rh-python36/root/bin/python3 -m pip install scikit-learn
sudo /opt/rh/rh-python36/root/bin/python3 -m pip install findspark

cd /opt/dev/src/main/jupyter

/opt/rh/rh-python36/root/bin/jupyter notebook --no-browser --allow-root --port=8889 --ip='*' --NotebookApp.token='' --NotebookApp.password='' &