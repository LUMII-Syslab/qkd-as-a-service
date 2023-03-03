#!/bin/bash

DIR=`dirname $0`
pushd $DIR/..

git remote add -f ca-scripts-upstream https://github.com/LUMII-Syslab/ca-scripts.git
git subtree add --prefix ca-scripts ca-scripts-upstream main --squash

popd
