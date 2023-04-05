#!/bin/bash

#export PATH=/usr/bin:/usr/local/bin:$PATH

if [ -z "$2" ]; then
  echo "Usage: $0 window-title command..."
  exit
fi

set -Eeuo pipefail

term() {
  export TERM=/usr/bin/gnome-terminal
  if [ -f $TERM ]; then
    # Linux
    $TERM -- bash -c "$@"
  else
    export TERM=/usr/bin/osascript
    if [ -f $TERM ]; then
      # macOS
      export CMD="tell app \"Terminal\" to do script \"$@\""
      $TERM -e "$CMD"
    else
      export TERMW="C:\\Windows\\System32\\cmd.exe"
      export TERMU=/cygdrive/c/Windows/System32/cmd.exe
      if [ -f $TERMU ]; then
        # Cygwin on Windows
        $TERMU /c start $TERMW /k "c:\\cygwin64\\bin\\bash.exe -c '$@'"
      fi
    fi
  fi
}


term_with_title_fn() {
  export TITLE=$1
  shift

  export TERM=/usr/bin/gnome-terminal
  if [ -f $TERM ]; then
    # Linux
#    term 'echo -n -e \\033]0\;'"$TITLE"'\\007 && '"$@"
    $TERM -- bash -c 'echo -n -e \\033]0\;'"$TITLE"'\\007 && '"$@"
  else
    export TERM=/usr/bin/osascript
    if [ -f $TERM ]; then
      # macOS
      term 'echo' '-n' '-e' '\\\\033]0\\;'"$TITLE"'\\\\007' '&&' $@
    else
      export TERMW="C:\\Windows\\System32\\cmd.exe"
      export TERMU=/cygdrive/c/Windows/System32/cmd.exe
      if [ -f $TERMU ]; then
        # Cygwin on Windows
        $TERMU /c start $TERMW /k "title $TITLE & c:\\cygwin64\\bin\\bash.exe -c '$@'"
      fi
    fi
  fi
}


# Removing spaces from the title (otherwise, echo -n -e \\033... does not work
export TITLE=$1
export TITLE=${TITLE/ /-}
term_with_title_fn $TITLE "${@:2}"



