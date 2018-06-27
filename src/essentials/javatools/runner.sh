#!/bin/bash

PROGRAMPATH=`dirname ${0}`

source ${PROGRAMPATH}/../../base.sh

MOD_CLASSPATH=${CLASSPATH}${PATHSEP}${PROGRAMPATH}/bin${PATHSEP}../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}../../libs/apache-commons/commons-math-2.1.jar${PATHSEP}../../libs/super-csv/super-csv-2.4.0.jar

ant -q -f "${PROGRAMPATH}/custom-build.xml" || exit 1

JAVA_OPTIONS="-server -Xbatch -XX:+AggressiveOpts -cp ${MOD_CLASSPATH} -Djava.library.path=/usr/lib/jvm/java-6-openjdk-amd64/jre/lib/amd64/server${PATHSEP}/usr/lib/jvm/java-6-openjdk-amd64/jre/lib/amd64${PATHSEP}/usr/lib/jvm/java-6-openjdk-amd64/jre/../lib/amd64${PATHSEP}/opt/xpressmp/lib/${PATHSEP}:/usr/java/packages/lib/amd64${PATHSEP}/usr/lib/x86_64-linux-gnu/jni${PATHSEP}/lib/x86_64-linux-gnu${PATHSEP}/usr/lib/x86_64-linux-gnu${PATHSEP}/usr/lib/jni${PATHSEP}/lib${PATHSEP}/usr/lib${PATHSEP}/usr/local/cplex/bin/x86-64_debian4.0_4.1"

#Determining the java version, needed to provide necessary adaption for java 9, that is incompatible to prior versions
#Source https://stackoverflow.com/questions/7334754/correct-way-to-check-java-version-from-bash-script
#Question by Aaron Digulla, https://stackoverflow.com/users/34088/aaron-digulla
#Answer by Glenn Jackman, https://stackoverflow.com/users/7552/glenn-jackman
#Adapted due to comment from khaemuaset, https://stackoverflow.com/users/1499546/khaemuaset
if type -p java; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    _java="$JAVA_HOME/bin/java"
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    padded_java_version=$(echo "$version" | awk -F. '{printf("%03d%03d",$1,$2);}')
    if [ $padded_java_version -ge 009000 ]; then
        JAVA_OPTIONS="--add-modules "java.xml.bind" ${JAVA_OPTIONS}"
    fi
fi


java ${JAVA_OPTIONS} net.lintim.main.${1} ${2}
