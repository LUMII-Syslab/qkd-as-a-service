#!/bin/bash

set -Eeuo pipefail

export MY_DIR=`dirname $0`
export CA_DIR=$MY_DIR/../ca-scripts

# initializing localhost proxy CA and signing proxy-aija, proxy-brencis and proxy-client-centis
#echo "===> Checking/generating the proxy CA key pair..."
#[ -d $CA_DIR/proxy-ca ] || $CA_DIR/ca_init.sh proxy-ca RSA $MY_DIR/proxy-ca.cnf
#echo "===> Checking/generating the Centis proxy client key pair..."
#[ -d $CA_DIR/proxy-client-centis ] || $CA_DIR/new_client_key.sh proxy-ca proxy-client-centis 3650
#echo "===> Checking/generating key pairs for proxy-aija and proxy-brencis..."
#[ -f $MY_DIR/proxy-aija.pem ] || $CA_DIR/new_server_key.sh proxy-ca $MY_DIR/server.cnf 3650 $MY_DIR/proxy-aija.pem
#[ -f $MY_DIR/proxy-brencis.pem ] || $CA_DIR/new_server_key.sh proxy-ca $MY_DIR/server.cnf 3650 $MY_DIR/proxy-brencis.pem

# initializing PQC CA for Centis and signing Centis client PQC certificate
echo "===> Checking/generating the PQC CA for Centis key pair..."
[ -d $CA_DIR/centis-ca ] || $CA_DIR/ca_init.sh centis-ca sphincssha256128frobust $MY_DIR/pqc-centis-ca.cnf
echo "===> Checking/generating the Centis client PQC key pair..."
[ -d $CA_DIR/centis ] || $CA_DIR/new_client_key.sh centis-ca centis $MY_DIR/pqc-centis.cnf

echo "===> Checking/generating the PQC CA (for Aija, Brencis, User1, User2) key pair..."
[ -d $CA_DIR/ca ] || $CA_DIR/ca_init.sh ca sphincssha256128frobust $MY_DIR/pqc-ca.cnf
echo "===> Checking/generating the User 1 and 2 client PQC key pairs..."
[ -d $CA_DIR/user1 ] || $CA_DIR/new_client_key.sh ca user1 $MY_DIR/pqc-user1.cnf
[ -d $CA_DIR/user2 ] || $CA_DIR/new_client_key.sh ca user2 $MY_DIR/pqc-user2.cnf

echo "===> Checking/generating the Aija and Brencis server PQC key pairs..."
[ -d $CA_DIR/aija ] || $CA_DIR/new_server_key.sh ca aija $MY_DIR/pqc-aija.cnf
[ -d $CA_DIR/brencis ] || $CA_DIR/new_server_key.sh ca brencis $MY_DIR/pqc-brencis.cnf

#if [ ! -f $MY_DIR/centis.pem ]; then
#  cat $CA_DIR/centis/client.crt > $MY_DIR/centis.pem
#  cat $CA_DIR/centis/client.key >> $MY_DIR/centis.pem
#fi

#if [ ! -f $MY_DIR/centis.pem ]; then
#  cat $CA_DIR/centis/client.crt > $MY_DIR/centis.pem
#  cat $CA_DIR/centis/client.key >> $MY_DIR/centis.pem
#fi

#if [ ! -f $MY_DIR/proxy-client-centis.pem ]; then
#  cat $CA_DIR/proxy-client-centis/client.crt > $MY_DIR/proxy-client-centis.pem
#  cat $CA_DIR/proxy-client-centis/client.key >> $MY_DIR/proxy-client-centis.pem
#fi
#if [ ! -f $MY_DIR/proxy-client-centis.pfx ]; then
#  cp $CA_DIR/proxy-client-centis/client.pfx $MY_DIR/proxy-client-centis.pfx
#fi

echo "===> PLEASE, INSTALL ${MY_DIR}/proxy-client-centis.pem (.pfx) AS YOUR CLIENT KEY!"
echo "===> Running HAProxy..."
echo "     connect to ws://localhost:8000/ws for aija"
#echo "     connect to ws://localhost:444/ws for brencis"
#/opt/oqs/sbin/haproxy -dL -V -f haproxy_rsa2oqs.cfg
