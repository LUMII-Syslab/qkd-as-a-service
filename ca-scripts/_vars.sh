#!/bin/bash

set -Eeuxo pipefail
# Here you can set up variables.
# The following vars are always set before calling this script (thus, we can use them here):
#   DIR
#   CA_NAME
# The following vars may also be set before calling this script
# (but if not set, the default values will be assigned):
#   for clients providing only CSR-s:
#     CLIENT_CRT - the file name of the signed certificate (usually, a .crt file)
#   for clients:
#     CLIENT_NAME
#     CLIENT_DAYS
#     CLIENT_ALIAS
#     CLIENT_KEYSTORE_PASS
#   for servers:
#     SERVER_NAME
#     SERVER_DAYS
#     SERVER_ALIAS
#     SERVER_KEYSTORE_PASS

#export OQS_OPENSSL=/opt/oqs/bin/openssl
export OQS_OPENSSL=`which openssl`
if [ -z $OQS_OPENSSL ]; then
  OQS_OPENSSL=/usr/local/bin/openssl
fi
export OQS_OPENSSL_FLAGS=
# For macOS, we may need to specify additional path for dylibs (@rpath):
#export DYLD_LIBRARY_PATH=/opt/oqs/lib

# For OpenSSL v3, we can use the open-quantum-safe provider:
#export OQS_OPENSSL=/usr/bin/openssl
#export OQS_OPENSSL_FLAGS="-provider oqsprovider -provider default"

export CA_KEY=$DIR/$CA_NAME/ca.key
export CA_CRT=$DIR/$CA_NAME/ca.crt
export CA_VARS=$DIR/$CA_NAME/ca_vars.sh
export CA_ALIAS=ca

export CA_DAYS=3999
export CA_TRUSTSTORE=$DIR/$CA_NAME/ca.truststore
export CA_TRUSTSTORE_PASS=ca-truststore-pass

export ALL_CA_PEM=$DIR/all-ca/ca.pem
export ALL_CA_TRUSTSTORE=$DIR/all-ca/ca.truststore
export ALL_CA_TRUSTSTORE_PASS=ca-truststore-pass

if [ ! -z "${CLIENT_CSR:-}" ]; then
  export CLIENT_DAYS=${CLIENT_DAYS:-365}
  export CLIENT_CRT=${CLIENT_CRT:-${CLIENT_CSR%.*}.crt}
elif [ ! -z "${CLIENT_NAME:-}" ]; then
  export CLIENT_DAYS=${CLIENT_DAYS:-365}
  export CLIENT_KEY=${DIR}/${CLIENT_NAME}/client.key
  export CLIENT_CSR=${DIR}/${CLIENT_NAME}/client.csr
  export CLIENT_CRT=${DIR}/${CLIENT_NAME}/client.crt
  export CLIENT_PEM=${DIR}/${CLIENT_NAME}/client.pem
         # ^^^ .pem will contain .crt + .key in the PEM (=Base64 DER) format
  export CLIENT_PFX=${DIR}/${CLIENT_NAME}/client.pfx
         # ^^^ .pfx will contain ca.pem + .crt + .key in the PKCS#12 format
  export CLIENT_ALIAS=${CLIENT_ALIAS:-client}
  export CLIENT_KEYSTORE=${DIR}/${CLIENT_NAME}/client.keystore
  export CLIENT_KEYSTORE_PASS=${CLIENT_KEYSTORE_PASS:-client-keystore-pass}
elif [ ! -z "${SERVER_NAME:-}" ]; then
  export SERVER_DAYS=${SERVER_DAYS:-365}
  export SERVER_KEY=${DIR}/${SERVER_NAME}/server.key
  export SERVER_CSR=${DIR}/${SERVER_NAME}/server.csr
  export SERVER_CRT=${DIR}/${SERVER_NAME}/server.crt
  export SERVER_PEM=${DIR}/${SERVER_NAME}/server.pem
         # ^^^ .pem will contain .crt + .key in the PEM (=Base64 DER) format
  export SERVER_PFX=${DIR}/${SERVER_NAME}/server.pfx
         # ^^^ .pfx will contain ca.pem + .crt + .key in the PKCS#12 format
  export SERVER_ALIAS=${SERVER_ALIAS:-server}
  export SERVER_KEYSTORE=${DIR}/${SERVER_NAME}/server.keystore
  export SERVER_KEYSTORE_PASS=${SERVER_KEYSTORE_PASS:-server-keystore-pass}
fi
