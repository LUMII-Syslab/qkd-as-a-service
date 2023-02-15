#!/bin/bash
#
# Generates a new client key (aka token) for client authentication.
# The token is intended to be validated by, e.g., HAProxy by checking that the token
# has been signed by our CA.
#
# Script arguments: ca-name client-name token-validity-in-days
#   (no spaces or special symbols in ca-name and client-name, please!)
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)

if [ "$3" = "" ]; then
  echo Usage: $0 ca-name client-name token-validity-in-days
  echo "  (no spaces or special symbols in ca-name and client-name, please!)"
  exit
fi
export CA_NAME=$1
export CLIENT_NAME=$2
export DAYS=$3
source $DIR/_vars.sh
source $CA_VARS

export DER_TMP=`dirname $CLIENT_KEY`/${CLIENT_NAME}.der.tmp
export PEM_TMP=`dirname $CLIENT_KEY`/${CLIENT_NAME}.pem.tmp

rm -r ${DIR}/${CLIENT_NAME}
mkdir ${DIR}/${CLIENT_NAME}

echo "Generating the client key pair for the user ${CLIENT_NAME}..."
${OQS_OPENSSL} req -new -newkey ${SIG_ALG} -keyout ${CLIENT_KEY} -out ${CLIENT_CSR} -nodes ${OQS_OPENSSL_REQ_ARGS} ${OQS_OPENSSL_FLAGS}
echo "Signing the client key pair for the user ${CLIENT_NAME}..."
${OQS_OPENSSL} x509 -req -in ${CLIENT_CSR} -out ${CLIENT_CRT} -CA ${CA_CRT} -CAkey ${CA_KEY} -CAcreateserial -days $DAYS ${OQS_OPENSSL_FLAGS}


cat ${CA_CRT} >${PEM_TMP}
cat $CLIENT_CRT >>${PEM_TMP}
cat $CLIENT_KEY >>${PEM_TMP}

echo "Importing the client key+cert+cacert to the DER format..."
${OQS_OPENSSL} x509 -in $PEM_TMP -inform pem -out $DER_TMP -outform der ${OQS_OPENSSL_FLAGS}
echo "Importing the client key+cert+cacert into Java key store..."
keytool -v -printcert -file $DER_TMP
echo yes | keytool -importcert -alias ${CLIENT_ALIAS} -keystore ${CLIENT_KEYSTORE} -storepass ${CLIENT_STOREPASS} -file $DER_TMP

# ${OQS_OPENSSL} pkcs12 -export -in ${PEM_TMP} \
#               -out ${CLIENT_KEYSTORE} -name ${CLIENT_ALIAS} \
#               -CAfile ${CA_CRT} -caname root -chain \
#               -password env:CLIENT_STOREPASS \
#               -noiter -nomaciter \
#               ${OQS_OPENSSL_FLAGS}

rm ${PEM_TMP}
rm ${DER_TMP}

echo "Validating..."
keytool -keystore ${CLIENT_KEYSTORE} -storepass ${CLIENT_STOREPASS} -v -list -storetype pkcs12 -alias ${CLIENT_ALIAS}


echo "We are done."
echo "Deployable files:"
echo " * ${CA_TRUSTSTORE} - CA trust store in the Java key store format"
echo "   OR ${CA_CRT} - CA self-signed certificate in the PEM (=Base64 DER) format"
echo " * ${CLIENT_KEYSTORE} - client secret key + certificate in the Java key store format"
echo "   OR ${CLIENT_KEY} WITH ${CLIENT_CRT} - client secret key + certificate the PEM (=Base64 DER) format"
