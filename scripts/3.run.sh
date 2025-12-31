#!/bin/bash

set -Eeuo pipefail

# obtain full path to the directory where this script is located
export MY_DIR=`dirname $0`
pushd $MY_DIR
export MYDIR=$PWD
popd

export BAT=
export BASH=/usr/bin/bash
export FILE_SEPARATOR="/"
if [ -v OS ]; then
  if [ "$OS" == "Windows_NT" ]; then
    export BAT=.bat
    export BASH="C:\\Windows\\System32\\bash.exe"
    export FILE_SEPARATOR="\\"
    export MYDIR=${MYDIR//"/cygdrive/c"/"C:"}
    export MYDIR=${MYDIR//"/cygdrive/d"/"D:"}
    export MYDIR=${MYDIR//"/"/"\\"}
  fi
fi

pushd $MY_DIR/../pqproxy
export PROJ_ROOT=$PWD
#export LD_LIBRARY_PATH=$PROJ_ROOT/.libs:/opt/oqs/lib
#export DYLD_LIBRARY_PATH=$PROJ_ROOT/.libs:/opt/oqs/lib

#$MY_DIR/../pqproxy/gradlew$BAT build

export JAVA_CP="$PROJ_ROOT/build/classes/java/main:$PROJ_ROOT/build/classes/java/test:$PROJ_ROOT/.jars/\\*"
if [ $(uname -s) == "Darwin" ]; then
  # we need additional backslash before the asterisk in macOS:
  export JAVA_CP="$PROJ_ROOT/build/classes/java/main:$PROJ_ROOT/build/classes/java/test:$PROJ_ROOT/.jars/\\\*"
fi
if [ -v OS ]; then
  if [ "$OS" == "Windows_NT" ]; then
    # use windows-style Java classpath on Windows...
    export JAVA_CP=${JAVA_CP//":"/";"}
    export JAVA_CP=${JAVA_CP//"/cygdrive/c"/"C:"}
    export JAVA_CP=${JAVA_CP//"/cygdrive/d"/"D:"}
    export JAVA_CP=${JAVA_CP//"/"/"\\"}
  fi
fi

export JAVA_LP="/opt/oqs/lib:/usr/local/lib:$PROJ_ROOT/.libs"
export MAIN_CLASS=lv.lumii.pqproxy.PQProxy

echo $JAVA_LP

##### TMUX-ing....

if tmux has-session -t butterfly 2>/dev/null; then
    tmux kill-session -t butterfly
fi

ARGS="-f $MYDIR/pqproxy-pqcuser2aija.properties"
CMD1="java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS ; exec bash"

ARGS="-f $MYDIR/pqproxy-pqcuser2brencis.properties"
CMD2="java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS; exec bash"

CMD3="cd $MYDIR/../service && go mod tidy && go run .; exec bash"

CMD4="exec bash"

tmux new-session -d -s butterfly \
  "$CMD1" \; \
  select-pane -T "one" \; \
  split-window -h "$CMD3" \; \
  select-pane -T "three" \; \
  split-window -v "$CMD4" \; \
  select-pane -T "four" \; \
  select-pane -t 0 \; \
  split-window -v "$CMD2" \; \
  select-pane -T "two" \; \
  select-layout tiled \; \
  set -g mouse on \; \
  set -g pane-border-status top \; \
  attach

exit

tmux new-session -d -s butterfly \
  'bash -c "echo 1; exec bash"' \; \
  select-pane -T "one" \; \
  split-window -v 'bash -c "echo 2; exec bash"' \; \
  select-pane -T "two" \; \
  split-window -h 'bash -c "echo 3; exec bash"' \; \
  select-pane -T "three" \; \
  select-pane -t 0 \; \
  split-window -v 'bash -c "echo 4; exec bash"' \; \
  select-pane -T "four" \; \
  select-layout tiled \; \
  set -g mouse on \; \
  set -g pane-border-status top \; \
  attach

exit
tmux new-session -d -s grid \
  'bash -c "echo 1; exec bash"' \; \
  split-window -h 'bash -c "echo 2; exec bash"' \; \
  split-window -v 'bash -c "echo 3; exec bash"' \; \
  select-pane -t 0 \; \
  split-window -v 'bash -c "echo 4; exec bash"' \; \
  select-layout tiled \; \
  set -g mouse on \; \
  attach
exit

export NAME="BUTTERFLY"
export ARGS="-f $MYDIR/pqproxy-pqcuser2aija.properties"
export CMD="exec bash" #"java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS ; exec bash"
tmux new-session -d -s butterfly -n $NAME '$CMD'
export CMD="java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS ; exec bash"
tmux send-keys -t butterfly "$CMD" ENTER
tmux set-option -t butterfly -g mouse on

export NAME="PQXPROXY-TO-BRENCIS"
export ARGS="-f $MYDIR/pqproxy-pqcuser2brencis.properties"
export CMD="exec bash" #"echo java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS; exec bash"
tmux split-window -t butterfly  -h '$CMD'
export CMD="echo java -cp $JAVA_CP -Djava.library.path=$JAVA_LP $MAIN_CLASS $ARGS; exec bash"
tmux send-keys -t butterfly "$CMD" ENTER

echo 4

export NAME="AIJA+BRENCIS"
export CMD="echo cd $MYDIR/../service && go mod tidy && go run .; exec bash"

tmux select-pane -t butterfly -t 0
tmux split-window -t butterfly -v  '$CMD'

export NAME="BASH"
export CMD="exec bash"

tmux select-pane -t butterfly -t 1
tmux split-window -t butterfly -v '$CMD'

tmux select-layout -t butterfly tiled
tmux attach -t butterfly


