#!/bin/sh

export FULL_VERSION=0.11.0
export SO_VERSION=6
CNFFILE=/etc/ssl/openssl.cnf
SUDO=sudo

wget https://qkd.lumii.lv/liboqs-binaries/Linux-x86_64/liboqs.so.$FULL_VERSION
wget https://qkd.lumii.lv/liboqs-binaries/Linux-x86_64/oqsprovider.so
wget https://qkd.lumii.lv/liboqs-binaries/Linux-x86_64/liboqs-jni.so
chmod +x *.so*
sudo mkdir -p /usr/local/lib/ossl-modules/

sudo mv liboqs.so.$FULL_VERSION /usr/local/lib/
sudo rm /usr/local/lib/liboqs.so.$SO_VERSION
sudo ln -s /usr/local/lib/liboqs.so.$FULL_VERSION /usr/local/lib/liboqs.so.$SO_VERSION
sudo rm /usr/local/lib/liboqs.so
sudo ln -s /usr/local/lib/liboqs.so.$SO_VERSION /usr/local/lib/liboqs.so
sudo mv liboqs-jni.so /usr/local/lib/

sudo mv oqsprovider.so /usr/local/lib/ossl-modules/


if [ -f $CNFFILE ]; then
  cp $CNFFILE $(basename $CNFFILE).bak
fi


sudo apt-get install crudini

$SUDO crudini --set $CNFFILE "" openssl_conf openssl_init
$SUDO crudini --set $CNFFILE openssl_init providers provider_sect
$SUDO crudini --set $CNFFILE provider_sect oqsprovider oqsprovider_sect
$SUDO crudini --set $CNFFILE provider_sect default default_sect
$SUDO crudini --set $CNFFILE oqsprovider_sect activate 1
$SUDO crudini --set $CNFFILE oqsprovider_sect module /usr/local/lib/ossl-modules/oqsprovider.so

$SUDO crudini --set $CNFFILE default_sect activate 1

openssl list -providers
