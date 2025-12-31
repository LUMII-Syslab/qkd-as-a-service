#!/bin/sh

export FULL_VERSION=0.15.0
export SO_VERSION=9
export PLATFORM=`uname -s`-`uname -m`
export PREFIX=/opt/homebrew
CNFFILE=$PREFIX/etc/openssl@3/openssl.cnf
SUDO=sudo

pip3 install crudini

wget https://qkd.lumii.lv/liboqs-binaries/$PLATFORM/liboqs-$FULL_VERSION.dylib
wget https://qkd.lumii.lv/liboqs-binaries/$PLATFORM/oqsprovider.dylib
wget https://qkd.lumii.lv/liboqs-binaries/$PLATFORM/liboqs-jni.dylib
chmod +x *.dylib*
sudo mkdir -p $PREFIX/lib/ossl-modules/

sudo mv liboqs-$FULL_VERSION.dylib $PREFIX/lib/
sudo rm $PREFIX/lib/liboqs.$SO_VERSION.dylib
sudo ln -s $PREFIX/lib/liboqs-$FULL_VERSION.dylib $PREFIX/lib/liboqs.$SO_VERSION.dylib
sudo rm $PREFIX/lib/liboqs.dylib
sudo ln -s $PREFIX/lib/liboqs.$SO_VERSION.dylib $PREFIX/lib/liboqs.dylib
sudo mv liboqs-jni.dylib $PREFIX/lib/

sudo mv oqsprovider.dylib $PREFIX/lib/ossl-modules/


if [ -f $CNFFILE ]; then
  cp $CNFFILE $(basename $CNFFILE).bak
fi


$SUDO crudini --set $CNFFILE "" openssl_conf openssl_init
$SUDO crudini --set $CNFFILE openssl_init providers provider_sect
$SUDO crudini --set $CNFFILE provider_sect oqsprovider oqsprovider_sect
$SUDO crudini --set $CNFFILE provider_sect default default_sect
$SUDO crudini --set $CNFFILE oqsprovider_sect activate 1
$SUDO crudini --set $CNFFILE oqsprovider_sect module /opt/homebrew/lib/ossl-modules/oqsprovider.dylib

$SUDO crudini --set $CNFFILE default_sect activate 1

#export DYLD_LIBRARY_PATH=$PREFIX/lib
#export OPENSSL_CONF=$CNFFILE
$PREFIX/bin/openssl list -providers
