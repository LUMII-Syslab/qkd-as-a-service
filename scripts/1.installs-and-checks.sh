#!/bin/bash

set -Eeuo pipefail

# tmux, openssl with pqc, ...

export DYLD_LIBRARY_PATH=/usr/local/lib
export PATH=/opt/homebrew/bin:$PATH
# ^^^ We are adding path to homebrew, since on macOS we advice using its openssl - it works with liboqs provider!
#     Please, configure oqsprovider in /opt/homebrew/etc/openssl@3/openssl.cnf manually.

export MY_DIR=`dirname $0`
export CA_DIR=$MY_DIR/../ca-scripts

# dos2unix is needed, if we are invoked from cygwin
if command -v "dos2unix" &> /dev/null; then
  dos2unix $CA_DIR/*.sh
fi

sudo apt install tmux

# todo liboqs-install@ubuntu.sh or @macos.sh
