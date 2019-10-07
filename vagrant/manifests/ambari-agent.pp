notify { "Provisioning default on ${hostname} node.": }

file { '/etc/hosts':
  ensure  => file,
  content => template('common/hosts')
}

#Java SDK
# ugly
file {['/usr/jdk64']: 
  ensure => 'directory',
}

file {"/usr/jdk64/jdk-8u60-linux-x64.tar.gz":
  ensure  => present,
  source  => "/var/www/html/common/jdk-8u60-linux-x64.tar.gz",
  owner   => "vagrant",
}

exec { "unzip_sdk":
  command     => "/bin/gunzip /usr/jdk64/jdk-8u60-linux-x64.tar.gz",
  cwd         => "/usr/jdk64",
  logoutput => true,
  require      => File["/usr/jdk64/jdk-8u60-linux-x64.tar.gz"]
}

exec { "untar_sdk":
  command     => "/bin/tar xf /usr/jdk64/jdk-8u60-linux-x64.tar",
  cwd         => "/usr/jdk64",
  logoutput => true,
  require      => Exec["unzip_sdk"]
}

service { 'firewalld':
  ensure => 'stopped',
  enable => 'false'
}

package { 'ntp' : 
  ensure => 'installed',
  allow_virtual => false,
}

service { 'ntpd':
  ensure 		=> running,
  enable 		=> true,
  require => Package['ntp']
}

#local YUM repo
package { 'httpd' : 
  ensure => 'installed',
  allow_virtual => false
}

package { 'yum-plugin-priorities' : 
  ensure => 'installed',
  allow_virtual => false
}

service { 'httpd':
  ensure 		=> running,
  enable 		=> true,
  require => Package['httpd']
}

yumrepo { 'ambari':
  baseurl => "http://localhost/ambari/centos7/2.7.4.0-118",
  descr => "AMBARI",
  enabled => 1,
  gpgcheck => 0,
  priority => 1,
  require => Service['httpd']
}

yumrepo { 'HDP-3.1-repo-1':
  baseurl => "http://localhost/hdp/HDP/centos7/3.1.4.0-315",
  descr => "HDP-3.1-repo-1",
  enabled => 1,
  gpgcheck => 0,
  priority => 1,
  require => Service['httpd']
}

yumrepo { 'HDP-UTILS-1.1.0.22-repo-1':
  baseurl => "http://localhost/hdp/HDP-UTILS/centos7/1.1.0.22",
  descr => "HDP-UTILS-1.1.0.22-repo-1",
  enabled => 1,
  gpgcheck => 0,
  priority => 1,
  require => Service['httpd']
}

#ambari agent
package { 'ambari-agent' : 
  ensure => 'installed',
  allow_virtual => false,
  require => [
    Yumrepo['ambari']
  ]
}

service { 'ambari-agent':
  ensure 		=> running,
  enable 		=> true,
#  require => File['/etc/ambari-agent/conf/ambari-agent.ini']
  require => Package['ambari-agent']
}





