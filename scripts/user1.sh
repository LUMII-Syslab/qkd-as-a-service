#!/bin/bash

set -Eeuo pipefail

# obtain full path to the directory where this script is located
export MY_DIR=`dirname $0`
pushd $MY_DIR
export MYDIR=$PWD
echo $MYDIR
popd

pushd $MY_DIR/../userlib+pqproxy
export PROJ_ROOT=$PWD
#export LD_LIBRARY_PATH=$PROJ_ROOT/.libs:/opt/oqs/lib
#export DYLD_LIBRARY_PATH=$PROJ_ROOT/.libs:/opt/oqs/lib
touch src/main/java/lv/lumii/pqproxy/PQProxy.java
./gradlew compileTestJava

export JAVA_CP="$PROJ_ROOT/build/classes/java/main:$PROJ_ROOT/build/classes/java/test:$PROJ_ROOT/.jars/\\*"
if [ $(uname -s) == "Darwin" ]; then
  # we need additional backslash before the asterisk in macOS:
  export JAVA_CP="$PROJ_ROOT/build/classes/java/main:$PROJ_ROOT/build/classes/java/test:$PROJ_ROOT/.jars/\\\*"
fi
export JAVA_LP="/opt/oqs/lib:/usr/local/lib:$PROJ_ROOT/.libs"
export MAIN_CLASS=lv.lumii.test.QkdTestUser1
export ARGS=

$MYDIR/term_with_title.sh "QaaS USER1" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS && bash"

popd
