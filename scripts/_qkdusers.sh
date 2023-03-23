#!/bin/bash


term() {
  export TERM=/usr/bin/gnome-terminal
  if [ -f $TERM ]; then
    $TERM -- bash -c "$@"
    exit
  fi
  export TERM=/usr/bin/osascript
  if [ -f $TERM ]; then
    export CMD = "tell app "Terminal" to do script \"$@\""
    $TERM -e "$CMD"
    exit
  fi
  export TERM=/cygdrive/c/Windows/System32/cmd.exe
  if [ -f $TERM ]; then
    $TERM /c start bash -c "$@"
  fi
}


export MY_DIR=`dirname $0`
pushd $MY_DIR/..
export LD_LIBRARY_PATH=$PWD/.libs:/opt/oqs/lib
export DYLD_LIBRARY_PATH=$PWD/.libs:/opt/oqs/lib
touch src/test/java/lv/lumii/test/QkdTestUser1.java
touch src/test/java/lv/lumii/test/QkdTestUser2.java
./gradlew compileTestJava

export JAVA_CP="build/classes/java/main:build/classes/java/test:.jars/*"
export JAVA_LP="/usr/local/lib:.libs"

export MAIN_CLASS=lv.lumii.test.QkdTestUser1
term "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

export MAIN_CLASS=lv.lumii.test.QkdTestUser2
term "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

popd

