#!/bin/bash

# exit on error and show commands
set -ex

# check for correct usage
if [-z "$3"]; then
    echo "Usage: $0 tmux-session tmux-window command"
    echo "Creates a window in session and runs the command"
    exit
fi

# assign variables
SESSION=$1
WINDOW=$2
COMMAND=$3
TMUX=$(which tmux)

# create tmux window and run command
$TMUX new-window -a -t $SESSION -n $WINDOW 'bash'
$TMUX send-keys -t $SESSION:$WINDOW "$COMMAND" ENTER
$TMUX select-window -t $SESSION:0

echo "Created new tmux window $WINDOW in session $SESSION"
