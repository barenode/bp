notify { "Provisioning ASCIIDOC node.": }

#exec {'download-gf':  
#  command => "/usr/bin/wget http://mirror.ghettoforge.org/distributions/gf/gf-release-latest.gf.el7.noarch.rpm -O /tmp/gf-release-latest.gf.el7.noarch.rpm" 
#}

#exec {'gf':  
#  command => "/usr/bin/rpm -Uvh /tmp/gf-release-latest.gf.el7.noarch.rpm",
#  require => Exec["download-gf"],
#}

#exec {'download-epel':  
#  command => "/usr/bin/wget http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm -O /tmp/epel-release-latest-7.noarch.rpm",
#  require => Exec["gf"],
#}
  
#exec {'epel':  
#  command => "/usr/bin/rpm -Uvh /tmp/epel-release-latest-7.noarch.rpm",
#  require => Exec["download-epel"],
#}                                                                      
   
#ruby stuff   
package { 'gcc-c++' :         ensure  => present}
package { 'patch' :           ensure  => present, require => Package["gcc-c++"]}
package { 'readline' :        ensure  => present, require => Package["patch"]}
package { 'readline-devel' :  ensure  => present, require => Package["readline"]}
package { 'zlib' :            ensure  => present, require => Package["readline-devel"]}
package { 'zlib-devel' :      ensure  => present, require => Package["zlib"]}
package { 'libyaml-devel' :   ensure  => present, require => Package["zlib-devel"]}
package { 'libffi-devel' :    ensure  => present, require => Package["libyaml-devel"]}
package { 'openssl-devel' :   ensure  => present, require => Package["libffi-devel"]}
package { 'make' :            ensure  => present, require => Package["openssl-devel"]}
package { 'bzip2' :           ensure  => present, require => Package["make"]}
package { 'autoconf' :        ensure  => present, require => Package["bzip2"]}
package { 'automake' :        ensure  => present, require => Package["autoconf"]}
package { 'libtool' :         ensure  => present, require => Package["automake"]}
package { 'bison' :           ensure  => present, require => Package["libtool"]}
package { 'sqlite-devel' :    ensure  => present, require => Package["bison"]}

#mathematical stuff
package { 'cmake' :           ensure  => present, require => Package["sqlite-devel"]}
package { 'bzip2-devel' :     ensure  => present, require => Package["cmake"]}
package { 'glib*' :           ensure  => present, require => Package["bzip2-devel"]}
package { 'cairo-devel' :     ensure  => present, require => Package["bzip2-devel"]}
package { 'pango-devel' :     ensure  => present, require => Package["cairo-devel"]}
package { 'gdk-pixbuf2-devel' : ensure  => present, require => Package["pango-devel"]}
package { 'libxml2-devel' :   ensure  => present, require => Package["gdk-pixbuf2-devel"]}
package { 'flex' :            ensure  => present, require => Package["libxml2-devel"]}
#package { 'lyx-fonts' :       ensure  => present, require => Package["flex"]}
package { 'ruby' :            ensure  => present, require => Package["flex"]}
#package { 'centos-release-scl' :   ensure  => present, require => Package["lyx-fonts"]}   
#package { 'rh-ruby25' :            ensure  => present, require => Package["centos-release-scl"]}
   
#exec {'mpapis.asc':  
#  command => "/bin/curl -sSL https://rvm.io/mpapis.asc | gpg --import -",
#  require => Package["lyx-fonts"],
#}

#exec {'get.rvm.io':  
#  command => "/bin/curl -L get.rvm.io | bash -s stable",
# require => Exec["mpapis.asc"],
#}  

#exec {'source':  
#  command => "/bin/bash -c 'source /etc/profile.d/rvm.sh'",
#  require => Exec["get.rvm.io"],
#} 

#exec {'rvm-reload':  
#  command => "/usr/local/rvm/bin/rvm reload",
#  require => Exec["source"],
#} 

#exec {'rvm-requirements':  
#  command => "/usr/local/rvm/bin/rvm requirements run",
#  require => Exec["rvm-reload"]
#}  

#exec {'rvm-install':  
#  command => "/usr/local/rvm/bin/rvm install 2.5",
#  require => Exec["rvm-requirements"]
#}

#exec {'rvm-use':  
#  command => "/usr/local/rvm/bin/rvm use 2.5 --default",
#  #command => "/usr/local/rvm/bin/rvm alias create default 2.5",
#  require => Exec["rvm-install"]
#}

#exec {'gem-asciidoctor':  command => "/usr/local/rvm/rubies/ruby-2.5.1/bin/gem install --no-ri --no-rdoc asciidoctor",                   require => Package["rh-ruby25"]}
#exec {'gem-asciidoctor-pdf':  command => "/usr/local/rvm/rubies/ruby-2.5.1/bin/gem install --no-ri --no-rdoc asciidoctor-pdf --pre",     require => Exec["gem-asciidoctor"]}
#exec {'gem-rouge':  command => "/usr/local/rvm/rubies/ruby-2.5.1/bin/gem install --no-ri --no-rdoc rouge",                               require => Exec["gem-asciidoctor-pdf"]}
#exec {'gem-pygments':  command => "/usr/local/rvm/rubies/ruby-2.5.1/bin/gem install --no-ri --no-rdoc pygments.rb",                      require => Exec["gem-rouge"]}
#exec {'gem-coderay':  command => "/usr/local/rvm/rubies/ruby-2.5.1/bin/gem install --no-ri --no-rdoc coderay",                           require => Exec["gem-pygments"]}
#exec {'gem-asciidoctor-mathematical':  command => "/usr/local/rvm/rubies/ruby-2.5.1/bin/gem install --no-ri --no-rdoc asciidoctor-mathematical",  require => Exec["gem-coderay"]}