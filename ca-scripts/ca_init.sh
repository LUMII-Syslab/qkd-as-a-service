#!/bin/bash
#
# Generates a new CA key pair to be used for signing server and client certificates.
# If the CA key pair already exists, does nothing.
#
# Script arguments: ca-alias
#   (no spaces or special symbols, please!)
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)
export CA_ALIAS=$1
if [ -z $CA_ALIAS ]; then
    echo -n "Please, specify the name (alias) of your CA [ca]: "
    read INP
    export CA_ALIAS=$INP
fi
if [ -z $CA_ALIAS ]; then
    export CA_ALIAS=ca
fi
source $DIR/_vars.sh

if [ -f $CA_KEY ] && [ -f $CA_TRUSTSTORE ]; then
  echo "Your CA has already been initialized."
  exit
fi

if [ -z $(which keytool) ]; then
    echo "Error: keytool not found. Please, install JDK and configure the PATH variable."
fi

if [ ! -f $OQS_OPENSSL ]; then
    echo "Error: No openssl found at '$OQS_OPENSSL'."
    echo "Specify the full path to openssl executable in OQS_OPENSSL."
    exit
fi
if [ -z $SIG_ALG ]; then
    echo "Error: No signature algorithm specified in SIG_ALG."
    echo "Use algorithm names from openssl."
    echo "For open-quantum-safe signature algorithms, use identifiers from the list at"
    echo "https://github.com/open-quantum-safe/openssl#authentication"
    echo "e.g., sphincssha256128frobust"
    exit
fi

# Temp file name - must be without the path since in Windows we use both cygwin executables and non-cygwin keytool
# (and /tmp does not exist on Windows)
export TMP=ca.der.tmp

mkdir -p `dirname $CA_KEY`
mkdir -p `dirname $CA_CRT`

echo "Generating CA key pair..."
${OQS_OPENSSL} req -x509 -new -newkey $SIG_ALG -keyout $CA_KEY -out $CA_CRT -nodes -days $CA_DAYS ${OQS_OPENSSL_REQ_ARGS}

echo "Converting CA certificate to the DER format..."
${OQS_OPENSSL} x509 -in $CA_CRT -inform pem -out $TMP -outform der
echo "Adding the certificate to the Java trust store..."
keytool -v -printcert -file $TMP
echo yes | keytool -importcert -alias ${CA_ALIAS} -keystore ${CA_TRUSTSTORE} -storepass ${CA_STOREPASS} -file $TMP
rm $TMP
echo "Validating..."
keytool -keystore ${CA_TRUSTSTORE} -storepass ${CA_STOREPASS} -list | grep ${CA_ALIAS}
echo "We are done."
echo "Deployable files:"
echo " * ${CA_TRUSTSTORE} - CA trust store in the Java key store format"
echo "   OR ${CA_CRT} - CA self-signed certificate in the PEM (=Base64 DER) format"
