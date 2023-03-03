#!/bin/bash

set -Eeuxo pipefail
#
# Generates a new CA key pair to be used for signing server and client certificates.
# If the CA key pair already exists, does nothing.
#
# Script arguments: [ca-name] [sig-alg] [ca-config-file] [openssl-ca-req-args-in-one-string]  [openssl-client-req-args-in-one-string] [openssl-server-req-args-in-one-string]
#   No spaces or special symbols in ca-name, please (default="ca").
#   For sig-alg, use one of the signature algorithms supported by your openssl ($OQS_OPENSSL), default="RSA".
#   E.g., if your openssl supports open-quantum-safe signature algorithms, use identifiers from the list found at
#   https://github.com/open-quantum-safe/openssl#authentication
#   e.g., sphincssha256128frobust
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)
export CA_NAME=${1:-}
export SIG_ALG=${2:-}
export CA_CONFIG_FILE=${3:-}
export OQS_OPENSSL_CA_REQ_ARGS=${4:-}
export OQS_OPENSSL_CLIENT_REQ_ARGS=${5:-}
export OQS_OPENSSL_SERVER_REQ_ARGS=${6:-}

if [ -z $CA_NAME ]; then
    echo -n "Please, specify the name of your CA [ca]: "
    read INP
    export CA_NAME=$INP
fi
if [ -z $CA_NAME ]; then
    export CA_NAME=ca
fi
source $DIR/_vars.sh
if [ -f $CA_VARS ]; then
  source $CA_VARS
fi
if [ -z $SIG_ALG ]; then
    $OQS_OPENSSL list -signature-algorithms $OQS_OPENSSL_FLAGS
    echo -n "Please, specify the signature algorithm [RSA]: "
    read INP
    export SIG_ALG=$INP
    echo -n "Enter additional openssl req args for the CA []: "
    read INP
    export OQS_OPENSSL_CA_REQ_ARGS=$INP
    echo -n "Enter additional openssl req args for clients []: "
    read INP
    export OQS_OPENSSL_CLIENT_REQ_ARGS=$INP
    echo -n "Enter additional openssl req args for servers []: "
    read INP
    export OQS_OPENSSL_SERVER_REQ_ARGS=$INP
fi
if [ -z $SIG_ALG ]; then
    export SIG_ALG=RSA
fi

if [ -z $CA_CONFIG_FILE ]; then
    echo -n "Enter OpenSSL CA config file [ca.cnf]: "
    read INP
    export CA_CONFIG_FILE=$INP
fi


if [ -f $CA_KEY ] && [ -f $CA_TRUSTSTORE ]; then
  echo "Your CA has already been initialized."
  exit
fi

if [ -z $(which keytool) ]; then
    echo "Error: keytool not found. Please, install JDK and configure the PATH variable."
fi

if [ ! -f $OQS_OPENSSL ]; then
    echo "Error: No openssl found at '$OQS_OPENSSL'."
    echo "Specify the full path to openssl executable in the _vars.sh file (OQS_OPENSSL)!"
    exit
fi
if [ -z $SIG_ALG ]; then
    echo "Error: No signature algorithm specified."
    exit
fi

# Temp file name - must be without the path since in Windows we use both cygwin executables and non-cygwin keytool
# (and /tmp does not exist on Windows)
export DER_TMP=ca.der.tmp

mkdir -p `dirname $CA_KEY`
mkdir -p `dirname $CA_CRT`
mkdir -p `dirname $CA_VARS`
mkdir -p `dirname $ALL_CA_TRUSTSTORE`

# Copying the CA config file (thus, the CA directory will contain all necessary info)
cp $CA_CONFIG_FILE `dirname $CA_KEY`/ca.cnf
export CA_CONFIG_FILE=`dirname $CA_KEY`/ca.cnf

# Remembering the signature algorithm (in order to use it for signing client and server certificates)...
echo "#!/bin/bash" > $CA_VARS
echo "" >> $CA_VARS
echo "export SIG_ALG=${SIG_ALG}" >> $CA_VARS
echo "export CA_CONFIG_FILE=\`dirname \$CA_KEY\`/ca.cnf" >> $CA_VARS
echo "export OQS_OPENSSL_CA_REQ_ARGS=${OQS_OPENSSL_CA_REQ_ARGS}" >> $CA_VARS
echo "export OQS_OPENSSL_CLIENT_REQ_ARGS=${OQS_OPENSSL_CLIENT_REQ_ARGS}" >> $CA_VARS
echo "export OQS_OPENSSL_SERVER_REQ_ARGS=${OQS_OPENSSL_SERVER_REQ_ARGS}" >> $CA_VARS
chmod +x $CA_VARS

echo "Generating CA key pair..."
${OQS_OPENSSL} req -x509 -new -newkey $SIG_ALG -keyout $CA_KEY -out $CA_CRT -nodes -days $CA_DAYS -config $CA_CONFIG_FILE ${OQS_OPENSSL_CA_REQ_ARGS} ${OQS_OPENSSL_FLAGS}
cat $CA_CRT >> $ALL_CA_PEM

echo "Converting CA certificate to the DER format..."
${OQS_OPENSSL} x509 -in $CA_CRT -inform pem -out $DER_TMP -outform der ${OQS_OPENSSL_FLAGS}
keytool -v -printcert -file $DER_TMP
echo "Adding the certificate to the Java trust store ${CA_TRUSTSTORE}..."
echo yes | keytool -importcert -alias ${CA_ALIAS} -keystore ${CA_TRUSTSTORE} -storepass ${CA_TRUSTSTORE_PASS} -file $DER_TMP
echo "Adding the certificate to the Java trust store ${ALL_CA_TRUSTSTORE}..."
echo yes | keytool -importcert -alias ${CA_NAME} -keystore ${ALL_CA_TRUSTSTORE} -storepass ${CA_TRUSTSTORE_PASS} -file $DER_TMP
rm $DER_TMP
echo "Validating..."
keytool -keystore ${CA_TRUSTSTORE} -storepass ${CA_TRUSTSTORE_PASS} -list | grep ${CA_ALIAS}
echo "All CA-s:"
keytool -keystore ${ALL_CA_TRUSTSTORE} -storepass ${CA_TRUSTSTORE_PASS} -list
echo "We are done."
echo "Deployable files:"
echo " * ${CA_TRUSTSTORE} - CA trust store in the Java key store format"
echo "   OR ${CA_CRT} - CA self-signed certificate in the PEM (=Base64 DER) format"
