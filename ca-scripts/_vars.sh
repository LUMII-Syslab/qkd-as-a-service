#!/bin/bash

# Here you can set up variables.
# The following vars are always set before calling this script (thus, we can use them here):
#   DIR
#   CA_NAME
# The following vars can also be set before calling this script:
#   CLIENT_NAME (for clients)
#   DAYS (for clients and servers)
#   SERVER_CONFIG_FILE (for servers)
#   SERVER_KEYPAIR_FILE (for servers)

#export OQS_OPENSSL=/opt/oqs/bin/openssl
# For macOS, we need to specify additional path for dylibs (@rpath):
#export DYLD_LIBRARY_PATH=/opt/oqs/lib

export OQS_OPENSSL=/usr/bin/openssl
export OQS_OPENSSL_FLAGS="-provider oqsprovider -provider default"

export CA_KEY=$DIR/$CA_NAME/ca.key
export CA_CRT=$DIR/$CA_NAME/ca.crt
export CA_VARS=$DIR/$CA_NAME/ca_vars.sh
export CA_ALIAS=ca

export CA_DAYS=3999
export CA_TRUSTSTORE=$DIR/$CA_NAME/ca.truststore
export CA_STOREPASS=ca-truststore-pass

export ALL_CA_PEM=$DIR/all-ca/ca.pem
export ALL_CA_TRUSTSTORE=$DIR/all-ca/ca.truststore
export ALL_CA_STOREPASS=ca-truststore-pass

export CLIENT_KEY=${DIR}/${CLIENT_NAME}/client.key
export CLIENT_CSR=${DIR}/${CLIENT_NAME}/client.csr
export CLIENT_CRT=${DIR}/${CLIENT_NAME}/client.crt
export CLIENT_ALIAS=client
export CLIENT_KEYSTORE=${DIR}/${CLIENT_NAME}/token.keystore
export CLIENT_STOREPASS=token-pass
