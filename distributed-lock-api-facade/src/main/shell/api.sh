#!/bin/sh

export appName=$appName

# Zookeeper Configuration
export hostPort="host1:port1,host2:port2"
export sessionTimeout=60000
export adminAuth=user:password

export cmd=${cmd}
export lockKey=${lockKey}
export timeout=${timeout}
export version="0.1.4"

nexus_base_path="https://maven.pkg.github.com/rainyheart"
jar="${nexus_base_path}/org/rainyheart/distributed-lock-api-facade/${version}/distributed-lock-api-facade-${version}.jar"

basepath=$(cd `dirname $0`;pwd)
download_target_jar=$basepath/distributed-lock-api-facade.jar

download_jar()
{
    echo "Download: $jar"
    wget ${jar} -q -O ${download_target_jar}
    
    rc=`echo $?`
    if [[ $rc -eq 0 ]];
    then
        echo "Download Successfully"
    else
        echo "Download Failed, exit now."
        exit 1
    fi
}

if [[ ${force_download} != '' ]]; then
    if [ -f ${download_target_jar} ]; then
        rm -f ${download_target_jar}
    fi
    download_jar
else
    if [ ! -f ${download_target_jar} ]; then
        download_jar
    fi
fi

chmod 744 ${download_target_jar}
if [[ ${cmd} == 'lock' ]]; then
    java -jar ${download_target_jar} "${cmd}" "${lockKey}" "${timeout}"
elif [[ ${cmd} == 'unlock' ]]; then
    java -jar ${download_target_jar} "${cmd}" "${lockKey}"
fi