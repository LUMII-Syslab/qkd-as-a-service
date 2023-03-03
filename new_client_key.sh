#!/bin/bash
#
# Generates a new client key (aka token) for client authentication.
# The token is intended to be validated by, e.g., HAProxy by checking that the token
# has been signed by our CA.
#
# Script arguments: ca-name client-name openssl-client-config-file
#   (no spaces or special symbols in ca-name and client-name, please!)
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)

if [ "$3" = "" ]; then
  echo Usage: $0 ca-name client-name openssl-client-config-file
  echo "  (no spaces or special symbols in ca-name and client-name, please!)"
  exit
fi
export CA_NAME=$1
export CLIENT_NAME=$2
export CLIENT_CONFIG_FILE=$3
source $DIR/_vars.sh
source $CA_VARS

mkdir -p `dirname ${CLIENT_KEY}`
mkdir -p `dirname ${CLIENT_CSR}`
mkdir -p `dirname ${CLIENT_CRT}`
mkdir -p `dirname ${CLIENT_PFX}`

echo "Generating the client key pair for the user ${CLIENT_NAME}..."
${OQS_OPENSSL} req -new -newkey ${SIG_ALG} -keyout ${CLIENT_KEY} -out ${CLIENT_CSR} -config ${CLIENT_CONFIG_FILE} -nodes ${OQS_OPENSSL_FLAGS} ${OQS_OPENSSL_CLIENT_REQ_ARGS}
echo "Signing the client key pair for the user ${CLIENT_NAME}..."
${OQS_OPENSSL} x509 -req -in ${CLIENT_CSR} -out ${CLIENT_CRT} -CA ${CA_CRT} -CAkey ${CA_KEY} -CAcreateserial -days $CLIENT_DAYS ${OQS_OPENSSL_FLAGS}



echo "Exporting to the PKCS#12 format..."
# ${OQS_OPENSSL} pkcs12 -export -in ${PEM_TMP} \
#               -out ${CLIENT_KEYSTORE} -name ${CLIENT_ALIAS} \
#               -CAfile ${CA_CRT} -caname root -chain \
#               -password env:CLIENT_STOREPASS \
#               -noiter -nomaciter \
#               ${OQS_OPENSSL_FLAGS}

# Creating a .pfx (PKCS#12) file (an alternative to .pem)...
${OQS_OPENSSL} pkcs12 -export -out ${CLIENT_PFX} \
		-password env:CLIENT_KEYSTORE_PASS \
		-name ${CLIENT_ALIAS} -caname root -nodes -noiter -nomaciter \
		-inkey $CLIENT_KEY -in $CLIENT_CRT -certfile $CA_CRT
		# ^^^ use -noenc instead of -nodes in newer versions of openssl


cat $CLIENT_CRT >${CLIENT_PEM}
cat $CLIENT_KEY >>${CLIENT_PEM}

echo "We are done."
echo "Deployable files:"
echo " * ${CA_TRUSTSTORE} - CA trust store in the Java key store format"
echo "   OR ${CA_CRT} - CA self-signed certificate in the PEM (=Base64 DER) format"
echo " * ${CLIENT_KEYSTORE} - client secret key + certificate in the Java key store format"
echo "   OR ${CLIENT_KEY} WITH ${CLIENT_CRT} - client secret key + certificate in the PEM (=Base64 DER) format"
echo "   OR ${CLIENT_PFX} - client secret key + certificate in the PKCS#12 format"

