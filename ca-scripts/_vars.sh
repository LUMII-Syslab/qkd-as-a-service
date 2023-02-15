#!/bin/bash

# Here you can set up variables.
# The following vars are set up before calling this script (thus, we can use them here):
# CA_ALIAS CLIENT_NAME DAYS CONFIG_FILE SERVER_KEYPAIR_FILE

export OQS_OPENSSL=/opt/oqs/bin/openssl
# For macOS, we need to specify additional path for dylibs (@rpath):
export DYLD_LIBRARY_PATH=/opt/oqs/lib

# The signature algorithm to use.
# For open-quantum-safe signature algorithms, use identifiers from the list found at
# https://github.com/open-quantum-safe/openssl#authentication
# e.g., sphincssha256128frobust
export SIG_ALG=sphincssha256128frobust


export CA_KEY=$DIR/$CA_ALIAS/ca.key
export CA_CRT=$DIR/$CA_ALIAS/ca.crt
export CA_DAYS=3999
export CA_TRUSTSTORE=$DIR/$CA_ALIAS/ca.truststore
export CA_STOREPASS=ca-truststore-pass

export OQS_OPENSSL_REQ_ARGS=

export CLIENT_KEY=${DIR}/${CLIENT_NAME}/client.key
export CLIENT_CSR=${DIR}/${CLIENT_NAME}/client.csr
export CLIENT_CRT=${DIR}/${CLIENT_NAME}/client.crt
export CLIENT_ALIAS=qrng_client
export CLIENT_KEYSTORE=${DIR}/${CLIENT_NAME}/token.keystore
export CLIENT_STOREPASS=token-pass
