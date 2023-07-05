#!/bin/bash

# exit on error
set -ex

# check for correct usage
if [ -z "$1" ]; then
    echo "Usage: $0 tmux-session"
    echo "Creates a new tmux session"
    exit
fi

# assign variables
SESSION=$1
WINDOW="info"
TMUX=$(which tmux)
SCRIPT_DIR=$(dirname $0)

# kill the previous session if it exists
if tmux has-session -t "$SESSION" 2>/dev/null; then
    $TMUX kill-session -t $SESSION
fi

# create tmux session
$TMUX new-session -d -s $SESSION -n $WINDOW 'bash'

# print information using cat
$TMUX send-keys -t $SESSION:$WINDOW "cat $SCRIPT_DIR/introduction.txt" ENTER

echo "Created new tmux session $SESSION"
