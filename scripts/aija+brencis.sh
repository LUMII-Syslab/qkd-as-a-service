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
export MAIN_CLASS=lv.lumii.pqproxy.PQProxy


if command -v tmux >/dev/null 2>&1; then
    # tmux is available on the system
    TMUX_SCRIPTS=$MYDIR/tmux

    $TMUX_SCRIPTS/tmux_new_session.sh "aija_brencis"

    # Launching PQC reverse proxies in front of Aija and Brencis (to be accessed from Users 1 and 2)
    export ARGS="-f $MYDIR/pqproxy-pqcuser2aija.properties"
    $TMUX_SCRIPTS/tmux_add_window.sh "aija_brencis" "USERS-REVERSE-PROXY-TO-AIJA" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    export ARGS="-f $MYDIR/pqproxy-pqcuser2brencis.properties"
    $TMUX_SCRIPTS/tmux_add_window.sh "aija_brencis" "USERS-REVERSE-PROXY-TO-BRENCIS" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    # Launching PQC reverse proxies in front of Aija and Brencis (to be accessed from Centis)
    export ARGS="-f $MYDIR/pqproxy-pqccentis2aija.properties"
    $TMUX_SCRIPTS/tmux_add_window.sh "aija_brencis" "CENTIS-REVERSE-PROXY-TO-AIJA" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    export ARGS="-f $MYDIR/pqproxy-pqccentis2brencis.properties"
    $TMUX_SCRIPTS/tmux_add_window.sh "aija_brencis" "CENTIS-REVERSE-PROXY-TO-BRENCIS" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    # Launching both Aija and Brencis
    $TMUX_SCRIPTS/tmux_add_window.sh "aija_brencis" "GO_AIJA_BRENCIS" "cd $MYDIR/..//service && go mod tidy && go run ."

    # Attach to the session
    $MYDIR/term_with_title.sh "aija+brencis.sh" "tmux attach -t aija_brencis"
else
    # Launching PQC reverse proxies in front of Aija and Brencis (to be accessed from Users 1 and 2)
    export ARGS="-f $MYDIR/pqproxy-pqcuser2aija.properties"
    $MYDIR/term_with_title.sh "USERS-REVERSE-PROXY-TO-AIJA" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    export ARGS="-f $MYDIR/pqproxy-pqcuser2brencis.properties"
    $MYDIR/term_with_title.sh "USERS-REVERSE-PROXY-TO-BRENCIS" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    # Launching PQC reverse proxies in front of Aija and Brencis (to be accessed from Centis)
    export ARGS="-f $MYDIR/pqproxy-pqccentis2aija.properties"
    $MYDIR/term_with_title.sh "CENTIS-REVERSE-PROXY-TO-AIJA" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    export ARGS="-f $MYDIR/pqproxy-pqccentis2brencis.properties"
    $MYDIR/term_with_title.sh "CENTIS-REVERSE-PROXY-TO-BRENCIS" "java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS"

    # Launching both Aija and Brencis
    $MYDIR/term_with_title.sh "GO AIJA+BRENCIS" "cd $MYDIR/..//service && go mod tidy && go run ."
fi

popd
