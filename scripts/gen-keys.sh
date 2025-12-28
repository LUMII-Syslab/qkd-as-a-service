#!/bin/bash

set -Eeuo pipefail

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

export PQC_SIG_ALG=sphincsshake128fsimple
# must be from:
#openssl list -signature-algorithms

# initializing PQC CA for Centis and signing Centis client PQC certificate
echo "===> Checking/generating the PQC CA for Centis key pair..."
[ -d $CA_DIR/centis-ca ] || $CA_DIR/ca_init.sh centis-ca $PQC_SIG_ALG $MY_DIR/pqc-centis-ca.cnf
echo "===> Checking/generating the Centis client PQC key pair..."
[ -d $CA_DIR/centis ] || $CA_DIR/new_client_key.sh centis-ca centis $MY_DIR/pqc-centis.cnf

echo "===> Checking/generating the PQC CA (for Aija, Brencis, User1, User2) key pair..."
[ -d $CA_DIR/ca ] || $CA_DIR/ca_init.sh ca $PQC_SIG_ALG $MY_DIR/pqc-ca.cnf
echo "===> Checking/generating the User 1 and 2 PQC key pairs..."
[ -d $CA_DIR/user1 ] || $CA_DIR/new_client_key.sh ca user1 $MY_DIR/pqc-user1.cnf
[ -d $CA_DIR/user2 ] || $CA_DIR/new_server_key.sh ca user2 $MY_DIR/pqc-user2.cnf

echo "===> Checking/generating the Aija and Brencis server PQC key pairs..."
[ -d $CA_DIR/aija ] || $CA_DIR/new_server_key.sh ca aija $MY_DIR/pqc-aija.cnf
[ -d $CA_DIR/brencis ] || $CA_DIR/new_server_key.sh ca brencis $MY_DIR/pqc-brencis.cnf

# Creating ECC keys for classical browsers.
# Important: do not use RSA, since RSA has been abandoned for signing ServerHello in TLSv1.3. Although RSA can be used
# for authentication, the TLSv1.3 ServerHello message is usually signed by the same server key.
 
# Initializing localhost proxy CA and signing proxy-aija, proxy-brencis and proxy-client-centis
echo "===> Checking/generating the proxy CA key pair (ECC, not RSA!)..."

export ALG=EC
export ALG_ARGS="-pkeyopt ec_paramgen_curve:P-256"
[ -d $CA_DIR/proxy-ca ] || $CA_DIR/ca_init.sh proxy-ca $ALG $MY_DIR/proxy-ca.cnf "$ALG_ARGS" "$ALG_ARGS" "$ALG_ARGS"
echo "===> Checking/generating the Centis proxy client key pair..."
[ -d $CA_DIR/proxy-client-centis ] || $CA_DIR/new_client_key.sh proxy-ca proxy-client-centis $MY_DIR/proxy-client.cnf
echo "===> Checking/generating key pairs for proxy-aija and proxy-brencis..."
[ -f $MY_DIR/proxy-aija.pem ] || $CA_DIR/new_server_key.sh proxy-ca proxy-aija $MY_DIR/proxy-server.cnf
[ -f $MY_DIR/proxy-brencis.pem ] || $CA_DIR/new_server_key.sh proxy-ca proxy-brencis $MY_DIR/proxy-server.cnf


[ ! -d $CA_DIR/proxy-client-centis ] || echo "===> PLEASE, INSTALL ${CA_DIR}/proxy-client-centis/proxy-client-centis.pem (.pfx) AS YOUR CLIENT KEY in your non-PQC browser where Centis will be used!"
[ ! -d $CA_DIR/proxy-ca ] || echo "===> PLEASE, INSTALL ${CA_DIR}/proxy-ca/ca.crt AS A TRUSTED CA FOR WEB-SITES in your non-PQC browser where Centis will be used!"
