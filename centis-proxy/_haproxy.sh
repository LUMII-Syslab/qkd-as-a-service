#!/bin/bash

export MY_DIR=`dirname $0`
export CA_DIR=$MY_DIR/../ca-scripts

# initializing localhost proxy CA and signing proxy-aija, proxy-brencis and proxy-client-centis
echo "===> Checking/generating the proxy CA key pair..."
[ -d $CA_DIR/proxy-ca ] || $CA_DIR/ca_init.sh proxy-ca RSA
echo "===> Checking/generating the Centis proxy client key pair..."
[ -d $CA_DIR/proxy-client-centis ] || $CA_DIR/new_client_key.sh proxy-ca proxy-client-centis 3650
echo "===> Checking/generating key pairs for proxy-aija and proxy-brencis..."
[ -f $MY_DIR/proxy-aija.pem ] || $CA_DIR/new_server_key.sh proxy-ca $MY_DIR/server.cnf 3650 $MY_DIR/proxy-aija.pem
[ -f $MY_DIR/proxy-brencis.pem ] || $CA_DIR/new_server_key.sh proxy-ca $MY_DIR/server.cnf 3650 $MY_DIR/proxy-brencis.pem

# initializing PQC CA for Centis and signing Centis client PQC certificate
echo "===> Checking/generating the PQC CA for Centis key pair..."
[ -d $CA_DIR/centis-ca ] || $CA_DIR/ca_init.sh centis-ca sphincssha256128frobust
echo "===> Checking/generating the Centis client PQC key pair..."
[ -d $CA_DIR/centis ] || $CA_DIR/new_client_key.sh centis-ca centis 3650

if [ ! -f $MY_DIR/centis.pem ]; then
  cat $CA_DIR/centis/client.crt > $MY_DIR/centis.pem
  cat $CA_DIR/centis/client.key >> $MY_DIR/centis.pem
fi

if [ ! -f $MY_DIR/centis.pem ]; then
  cat $CA_DIR/centis/client.crt > $MY_DIR/centis.pem
  cat $CA_DIR/centis/client.key >> $MY_DIR/centis.pem
fi

if [ ! -f $MY_DIR/proxy-client-centis.pem ]; then
  cat $CA_DIR/proxy-client-centis/client.crt > $MY_DIR/proxy-client-centis.pem
  cat $CA_DIR/proxy-client-centis/client.key >> $MY_DIR/proxy-client-centis.pem
fi
if [ ! -f $MY_DIR/proxy-client-centis.pfx ]; then
  cp $CA_DIR/proxy-client-centis/client.pfx $MY_DIR/proxy-client-centis.pfx
fi

echo "===> PLEASE, INSTALL ${MY_DIR}/proxy-client-centis.pem (.pfx) AS YOUR CLIENT KEY!"
echo "===> Running HAProxy..."
echo "     connect to ws://localhost:443/ws for aija"
echo "     connect to ws://localhost:444/ws for brencis"
/opt/oqs/sbin/haproxy -dL -V -f haproxy_rsa2oqs.cfg
