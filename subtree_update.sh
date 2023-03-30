#!/bin/bash

DIR=`dirname $0`
pushd $DIR/..

git fetch ca-scripts-upstream main
git subtree pull --prefix ca-scripts ca-scripts-upstream main --squash

popd