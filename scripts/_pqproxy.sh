#!/bin/bash

export MY_DIR=`dirname $0`
pushd $MY_DIR/..
export LD_LIBRARY_PATH=$PWD/.libs:/opt/oqs/lib
export DYLD_LIBRARY_PATH=$PWD/.libs:/opt/oqs/lib
touch src/main/java/lv/lumii/pqproxy/PQProxy.java
./gradlew pqproxy
popd
