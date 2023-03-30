#!/bin/bash

set -Eeuo pipefail

term() {
  export TERM=/usr/bin/gnome-terminal
  if [ -f $TERM ]; then
    $TERM -- bash -c "$@"
  else
    export TERM=/usr/bin/osascript
    if [ -f $TERM ]; then
      export CMD="tell app \"Terminal\" to do script \"$@\""
      $TERM -e "$CMD"
    else
      export TERM=/cygdrive/c/Windows/System32/cmd.exe
      if [ -f $TERM ]; then
       $TERM /c start bash -c "$@"
      fi
    fi
  fi
}


export MY_DIR=`dirname $0`
pushd $MY_DIR/..
export PROJ_ROOT=$PWD
#export LD_LIBRARY_PATH=$PROJ_ROOT/.libs:/opt/oqs/lib
#export DYLD_LIBRARY_PATH=$PROJ_ROOT/.libs:/opt/oqs/lib
touch src/main/java/lv/lumii/pqproxy/PQProxy.java
./gradlew compileTestJava

export JAVA_CP="$PROJ_ROOT/build/classes/java/main:$PROJ_ROOT/build/classes/java/test:$PROJ_ROOT/.jars/\\\*"
export JAVA_LP="/opt/oqs/lib:/usr/local/lib:$PROJ_ROOT/.libs"
export MAIN_CLASS=lv.lumii.pqproxy.PQProxy

export ARGS="-f $PROJ_ROOT/scripts/pqproxy-pqcuser2aija.properties"
term "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

export ARGS="-f $PROJ_ROOT/scripts/pqproxy-pqcuser2brencis.properties"
term "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

term "cd $PROJ_ROOT/../qkd-as-a-service/service && go run ."

popd
