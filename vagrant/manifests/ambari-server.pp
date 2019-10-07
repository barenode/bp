notify { "Provisioning master on ${hostname} node.": }

package { 'mc' : 
  ensure => 'installed',
  allow_virtual => false,
}

package {'unzip' : ensure => 'installed',  allow_virtual => false }
package {'curl' : ensure => 'installed',  allow_virtual => false }
package {'hdp-select' : ensure => 'installed',  allow_virtual => false }
package {'hadoop_3_1_4_0_315' : ensure => 'installed',  allow_virtual => false }
package {'hadoop_3_1_4_0_315-client' : ensure => 'installed',  allow_virtual => false }
package {'hadoop_3_1_4_0_315-hdfs' : ensure => 'installed',  allow_virtual => false }
package {'hadoop_3_1_4_0_315-yarn' : ensure => 'installed',  allow_virtual => false }
package {'hadoop_3_1_4_0_315-mapreduce' : ensure => 'installed',  allow_virtual => false }
package {'snappy' : ensure => 'installed',  allow_virtual => false }
package {'snappy-devel' : ensure => 'installed',  allow_virtual => false }
package {'hadoop_3_1_4_0_315-libhdfs' : ensure => 'installed',  allow_virtual => false }
package {'spark2_3_1_4_0_315-python' : ensure => 'installed',  allow_virtual => false }
package {'livy2_3_1_4_0_315' : ensure => 'installed',  allow_virtual => false }
package {'spark-atlas-connector_3_1_4_0_315' : ensure => 'installed',  allow_virtual => false }
package {'zookeeper_3_1_4_0_315-server' : ensure => 'installed',  allow_virtual => false }


package { 'ambari-server' : 
  ensure => 'installed',
  allow_virtual => false,
  require => [
    Package['unzip'],
    Package['curl'],
    Package['hdp-select'],
    Package['hadoop_3_1_4_0_315'],
    Package['hadoop_3_1_4_0_315-client'],
    Package['hadoop_3_1_4_0_315-hdfs'],
    Package['hadoop_3_1_4_0_315-yarn'],
    Package['hadoop_3_1_4_0_315-mapreduce'],
    Package['snappy'],
    Package['snappy-devel'],
    Package['hadoop_3_1_4_0_315-libhdfs'],
    Package['spark2_3_1_4_0_315-python'],
    Package['spark2_3_1_4_0_315-python'],
    Package['livy2_3_1_4_0_315'],
    Package['spark-atlas-connector_3_1_4_0_315'],
    Package['zookeeper_3_1_4_0_315-server']
  ]
}

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
  require => Service["ambari-server"]
}

exec { 'ambari-server-setup-mysql':
  command => '/usr/sbin/ambari-server setup --jdbc-db=mysql --jdbc-driver=/var/www/html/common/mysql-connector-java.jar',
  logoutput => true,
  require => [
    Exec['wait-for-ambari-server']     
  ]
}

exec {'vdf':
  command => '/usr/bin/curl -v -k -u admin:admin -H "X-Requested-By:ambari" -X POST http://localhost:8080/api/v1/version_definitions -d @/tmp/vagrant-puppet/blueprints/vdf.json', 
  logoutput => true,
  require => Exec['ambari-server-setup-mysql']
}

exec {'blueprint':
  command => '/usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://c7201.barenode.org:8080/api/v1/blueprints/blueprint -d @/tmp/vagrant-puppet/blueprints/blueprint-singlenode.json', 
  logoutput => true,
  require => [             
     Exec['vdf'] 
  ]
}

exec {'cluster':
  command => '/usr/bin/curl -H "X-Requested-By: ambari" -X POST -u admin:admin http://c7201.barenode.org:8080/api/v1/clusters/cluster -d @/tmp/vagrant-puppet/blueprints/cluster-singlenode.json', 
  logoutput => true,
  require => Exec['blueprint']
}


