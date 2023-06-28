#! /bin/bash

# the script
# - builds the service
# - copies the executable onto aija in configured users home directory
# - removes build artifacts
# aija has to be configured as one of known hosts in ~/.ssh/config
# note that to execute the built binary a few shared objects are needed

EXECUTABLE_NAME=qkdc-service

set -e # exit on first error
set -x # print commands

# obtain full path to the directory where this script is located
export MY_DIR=`dirname $0`
pushd $MY_DIR
export MYDIR=$PWD
echo $MYDIR
popd

# service directory
pushd $MY_DIR/../service
export SERVICE_DIR=$PWD
popd

# create temporary directory for building the service
TMP_DIR=/tmp/qkdc-service-build
mkdir -p $TMP_DIR

# build go executable
pushd $SERVICE_DIR
go mod tidy
go build -o $TMP_DIR/$EXECUTABLE_NAME .
popd

# copy executable onto one of kdcs
scp $TMP_DIR/$EXECUTABLE_NAME aija:~/$EXECUTABLE_NAME

# remove build artifacts
rm -rf $TMP_DIR