#!/bin/bash

if [ -z "$3" ]; then
  echo "Usage: $0 window-title working-directory command..."
  exit
fi

set -Eeuo pipefail

mac_term() {
    export TERM=/usr/bin/osascript
    if [ -f $TERM ]; then
      # macOS
      export CMD="tell app \"Terminal\" to do script \"$@\""
      $TERM -e "$CMD"
    fi
}


term_with_title_and_dir_fn() {
# launches Unix-style term on Linux, Terminal on macOS, and cmd on windows
  export TITLE=$1
  export WORKING_DIR=$2
  shift
  shift

  export ARGS="$@"

  export TERM=/usr/bin/gnome-terminal
  if [ -f $TERM ]; then
    # Linux
    $TERM -- bash -c 'echo -n -e \\033]0\;'"$TITLE"'\\007 && cd '"$WORKING_DIR"' && '"$ARGS"
  else
    export TERM=/usr/bin/osascript
    if [ -f $TERM ]; then
      # macOS
      mac_term 'echo' '-n' '-e' '\\\\033]0\\;'"$TITLE"'\\\\007' '&&' 'cd' "$WORKING_DIR" '&&' $@
    else
      export TERMW="C:\\Windows\\System32\\cmd.exe"
      export TERMU=/cygdrive/c/Windows/System32/cmd.exe
      if [ ! -f $TERMU ]; then
        # in WSL2, we do not have /cygdrive but cmd.exe works!
        export TERMU=cmd.exe
      fi
      # cmd on Windows
      $TERMU /c start $TERMW /k "title $TITLE & cd "$WORKING_DIR" & $ARGS"
    fi
  fi
}

# Removing spaces from the title (otherwise, echo -n -e \\033... does not work
export TITLE=$1
export TITLE=${TITLE/ /-}
export WORKING_DIR=$2
term_with_title_and_dir_fn $TITLE $WORKING_DIR ${@:3}
