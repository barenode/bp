notify { "Provisioning master on ${hostname} node.": }

package { 'mc' : 
  ensure => 'installed',
  allow_virtual => false,
}

package { 'ambari-server' : 
  ensure => 'installed',
  allow_virtual => false,
}

#file { '/etc/yum.repos.d/ambari-hdp-1.repo':
#  ensure  => file,
#  content => template('common/ambari-hdp-1.repo'),
#  require => Package['ambari-server']
#}

#file { '/var/lib/ambari-server/resources/stacks/HDP/3.1/repos/repoinfo.xml':
#file { '/tmp/repoinfo.xml':
#  ensure  => file,
#  content => template('common/ambari-hdp-1.repo'),
#  require => Package['ambari-server']
#}

#exec {'remove_repoinfo':
#	command 			=> "/bin/rm /var/lib/ambari-server/resources/stacks/HDP/3.1/repos/repoinfo.xml",
#  cwd     			=> '/tmp',
#  logoutput 		=> true,
#	require      	=> Package['ambari-server']
#}

#exec {'copy_repoinfo':
#	command 			=> "/bin/cp /tmp/repoinfo.xml /var/lib/ambari-server/resources/stacks/HDP/3.1/repos/repoinfo.xml",
#  cwd     			=> '/tmp',
#  logoutput 		=> true,
#	require      	=> Exec['remove_repoinfo']
#}

exec { 'ambari-server-setup':
  command => '/usr/sbin/ambari-server setup --verbose --silent --java-home /usr/jdk64/jdk1.8.0_60',
  logoutput => true,
  require => [
    Package['ambari-server']
  ]
}

service { 'ambari-server':
  ensure 		=> running,
  enable 		=> true,
  require => [
    Package['ambari-server'],
    Exec['ambari-server-setup']
  ]
}

exec {'wait-for-ambari-server':  
  command => "/usr/bin/wget --spider --tries 10 --retry-connrefused --no-check-certificate http://c7201.barenode.org:8080",
  require => Service["ambari-server"],
}

#exec {'local_hdp':
#  command => '/usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin -X PUT -d @/tmp/vagrant-puppet/blueprints/hdp_repo.json http://c7201.barenode.org:8080/api/v1/stacks/HDP/versions/3.1/operating_systems/redhat7/repositories/HDP-3.1', 
#  logoutput => true,
#  require => Exec['wait-for-ambari-server']
#}

#exec {'local_hdp_utils':
#  command => '/usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin -X PUT -d @/tmp/vagrant-puppet/blueprints/hdp_utils_repo.json http://c7201.barenode.org:8080/api/v1/stacks/HDP/versions/3.1/operating_systems/redhat7/repositories/HDP-UTILS-1.1.0.22', 
#  logoutput => true,
#  require => Exec['wait-for-ambari-server']
#}

exec { 'ambari-server-setup-mysql':
  command => '/usr/sbin/ambari-server setup --jdbc-db=mysql --jdbc-driver=/var/www/html/mysql-connector-java.jar',
  logoutput => true,
  require => [
    Exec['wait-for-ambari-server']     
  ]
}

exec {'blueprint':
  command => '/usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://c7201.barenode.org:8080/api/v1/blueprints/blueprint -d @/tmp/vagrant-puppet/blueprints/blueprint-multinode.json', 
  logoutput => true,
  require => [             
     Exec['wait-for-ambari-server'],
     Exec['ambari-server-setup-mysql']     
  ]
}

exec {'cluster':
  command => '/usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://c7201.barenode.org:8080/api/v1/clusters/cluster -d @/tmp/vagrant-puppet/blueprints/cluster-multinode.json', 
  logoutput => true,
  require => Exec['blueprint']
}


