#!/bin/bash

set -Eeuo pipefail

# obtain full path to the directory where this script is located
export MY_DIR=`dirname $0`
pushd $MY_DIR
export SCRIPT_DIR=$PWD
echo $SCRIPT_DIR
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
export MAIN_CLASS=lv.lumii.pqproxy.PQProxy

# Launching PQC forward proxies in front of Aija and Brencis (to be accessed from Users 1 and 2)

if command -v tmux >/dev/null 2>&1; then
    # tmux is available on the system
    TMUX_SCRIPTS=$SCRIPT_DIR/tmux

    $TMUX_SCRIPTS/tmux_new_session.sh "centis"

    export ARGS="-f $SCRIPT_DIR/pqproxy-local2pqccentis4aija.properties"
    $TMUX_SCRIPTS/tmux_add_window.sh "centis" "centis-forward-proxy-to-aija" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    export ARGS="-f $SCRIPT_DIR/pqproxy-local2pqccentis4brencis.properties"
    $TMUX_SCRIPTS/tmux_add_window.sh "centis" "centis-forward-proxy-to-brencis" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    $TMUX_SCRIPTS/tmux_add_window.sh "centis" "centis-backend" "cd $SCRIPT_DIR/../centis && npm install && npm start"

    $SCRIPT_DIR/term_with_title.sh "centis.sh" "tmux attach -t centis"
else
    # tmux is not available on the system
    export ARGS="-f $SCRIPT_DIR/pqproxy-local2pqccentis4aija.properties"
    $SCRIPT_DIR/term_with_title.sh "CENTIS-FORWARD-PROXY-TO-AIJA" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    export ARGS="-f $SCRIPT_DIR/pqproxy-local2pqccentis4brencis.properties"
    $SCRIPT_DIR/term_with_title.sh "CENTIS-FORWARD-PROXY-TO-BRENCIS" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    # Launching Centis
    $SCRIPT_DIR/term_with_title.sh "CENTIS BACKEND" "cd $SCRIPT_DIR/../centis && npm install && npm start"
fi

popd
