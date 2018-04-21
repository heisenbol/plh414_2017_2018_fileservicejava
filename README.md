# plh414_2017_2018_fileservicejava
plh414 2017/2018 File Service Sample Java demonstrating publication to Zookeeper instance using ephemeral node

Needs external servlet-api.jar to compile

For deployment, needs file WEB-INF/config.properties

zookeeper.jar can be retrieved from /usr/share/java/zookeeper.jar and has to be put in the lib folder

log4j-1.2.jar from /usr/share/java/log4j-1.2.jar

slf4j-api.jar from /usr/share/java/slf4j-api.jar


Sample deployment script

```
#!/bin/bash
if [ "$#" -ne 9 ]; then
    echo "Illegal number of parameters"
    echo "deploy.sh FSID ZOOKEEPER_HOST ZOOKEEPER_USERNAME ZOOKEEPER_PASSWORD SERVERHOSTNAME SERVER_PORT SERVER_SCHEME HMACKEY CONTEXT"
    echo "e.g. ../deploy.sh  FSX snf-xxxxx.vm.okeanos.grnet.gr username password snf-yyyyyy.vm.okeanos.grnet.gr 8080 http KEY fileservicejava"
    exit -1
fi
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo $DIR

echo ID=$1 > "$DIR/WEB-INF/config.properties"
echo ZOOKEEPER_HOST=$2 >> "$DIR/WEB-INF/config.properties"
echo ZOOKEEPER_USER=$3 >> "$DIR/WEB-INF/config.properties"
echo ZOOKEEPER_PASSWORD=$4 >> "$DIR/WEB-INF/config.properties"
echo SERVER_PORT=$6 >> "$DIR/WEB-INF/config.properties"
echo SERVER_SCHEME=$7 >> "$DIR/WEB-INF/config.properties"
echo HMACKEY=$8 >> "$DIR/WEB-INF/config.properties"
echo CONTEXT=$9 >> "$DIR/WEB-INF/config.properties"

cd $DIR/WEB-INF \
&& javac -Xlint:deprecation -Xlint:unchecked -cp /home/sk/Documents/isc/katanemimena2018/servlet-api.jar:lib/*:classes -d classes src/tuc/sk/*.java \
&& rsync -a -v --exclude deploy.sh --exclude .gitignore --exclude .git --delete $DIR/ root@$5:/var/lib/tomcat8/webapps/fileservicejava \
&& ssh root@$5 "chown -R tomcat8:tomcat8 /var/lib/tomcat8/webapps"
cd $DIR
#http://snf-814985.vm.okeanos.grnet.gr:8080/fileservicejava/file/aaa.png
```
