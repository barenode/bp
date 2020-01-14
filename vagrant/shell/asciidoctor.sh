
#!/bin/bash

#common stuff
yum install -y 'wget'

#asciidoctor stuff
yum install -y 'gcc-c++'
yum install -y 'patch'
yum install -y 'readline'
yum install -y 'readline-devel'
yum install -y 'zlib'
yum install -y 'zlib-devel'
yum install -y 'libyaml-devel'
yum install -y 'libffi-devel'
yum install -y 'openssl-devel'
yum install -y 'make'
yum install -y 'bzip2'
yum install -y 'autoconf'
yum install -y 'automake'
yum install -y 'libtool'
yum install -y 'bison'
yum install -y 'sqlite-devel'

#mathematical stuff
yum install -y 'cmake'
yum install -y 'bzip2-devel'
yum install -y 'glib*'
yum install -y 'cairo-devel'
yum install -y 'cairo-gobject'
yum install -y 'cairo-gobject-devel'
yum install -y 'pango-devel'
yum install -y 'gdk-pixbuf2-devel'
yum install -y 'libxml2-devel'
yum install -y 'flex'
yum install -y 'ruby'
yum install -y 'pandoc'

curl -sSL https://rvm.io/mpapis.asc | gpg2 --import -
curl -sSL https://rvm.io/pkuczynski.asc | gpg --import -
curl -L get.rvm.io | bash -s stable

source /etc/profile.d/rvm.sh
rvm reload
rvm requirements run
rvm install 2.5
rvm use 2.5 --default

gem install --no-document asciidoctor
gem install --no-document asciidoctor-pdf --pre
gem install --no-document rouge
gem install --no-document pygments.rb
gem install --no-document coderay
gem install --no-document asciidoctor-mathematical

wget http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm -O /tmp/epel-release-latest-7.noarch.rpm
rpm -Uvh /tmp/epel-release-latest-7.noarch.rpm
yum -y install lyx-fonts