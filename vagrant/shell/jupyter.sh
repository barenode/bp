#!/bin/bash

cd /opt/dev/src/main/jupyter

# /opt/rh/rh-python36/root/bin/jupyter notebook --no-browser --allow-root --port=8889 --ip='*' --NotebookApp.token='' --NotebookApp.password='' &

echo 'starting jupyter notebook'

sudo su vagrant -c '/opt/rh/rh-python36/root/bin/jupyter notebook --debug --no-browser --port=8889 --ip="*" --NotebookApp.token="" --NotebookApp.password="" &'

echo 'jupyter notebook started'

