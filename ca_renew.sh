#!/bin/bash

set -Eeuxo pipefail
#
# Generates a new CA key pair to be used for signing server and client certificates.
# The previous CA key pair (if any) is backed up.
#
# Script arguments: [ca-name]
#   (no spaces or special symbols, please!)
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)
export CA_NAME=${1:-}
if [ -z $CA_NAME ]; then
    echo -n "Please, specify the name of your CA [ca]: "
    read INP
    export CA_NAME=$INP
fi
if [ -z $CA_NAME ]; then
    export CA_NAME=ca
fi
source $DIR/_vars.sh
if [ -f $CA_VARS ]; then
  source $CA_VARS
fi

export TODAY=$(date +"%Y-%m-%d")
export TIMESTAMP=$(date +"%s")

if [ -f $CA_KEY ]; then
  echo "Backing up the current CA key pair..."  
  # Computing the backup dir $BACKUP_DIR...
  export BACKUP_DIR=$(dirname $CA_KEY)/$TODAY
  if [ -d $BACKUP_DIR ]; then
    export BACKUP_DIR=$(dirname $CA_KEY)/$TODAY-$TIMESTAMP
  fi
  mkdir -p $BACKUP_DIR

  # Backing up...
  mv $CA_KEY $BACKUP_DIR
  mv $CA_CRT $BACKUP_DIR
  mv $CA_TRUSTSTORE $BACKUP_DIR
fi

$DIR/ca_init.sh $CA_NAME $SIG_ALG $CA_CONFIG_FILE
