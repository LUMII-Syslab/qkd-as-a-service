#!/bin/bash
#
# Generates a new server key (to be used within, e.g., HAProxy, Apache, or nginx).
# Script arguments: ca-name server-name openssl-server-config-file
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)

if [ "$3" = "" ]; then
  echo Usage: $0 ca-name server-name openssl-server-config-file
  echo "  (If you are using our server.cnf as a template for openssl-config-file, please, edit it"
  echo "   to reflect your organization name, IP, and domain name!)"
  exit
fi

export CA_NAME=$1
export SERVER_NAME=$2
export SERVER_CONFIG_FILE=$3
source $DIR/_vars.sh
source $CA_VARS

mkdir -p `dirname $SERVER_KEY`
mkdir -p `dirname $SERVER_CSR`
mkdir -p `dirname $SERVER_CRT`
mkdir -p `dirname $SERVER_PEM`

${OQS_OPENSSL} req -new -newkey ${SIG_ALG} -keyout ${SERVER_KEY} -out ${SERVER_CSR} -nodes -config ${SERVER_CONFIG_FILE} ${OQS_OPENSSL_SERVER_REQ_ARGS}
${OQS_OPENSSL} x509 -req -in server.csr -out ${SERVER_CRT} -CA ${CA_CRT} -CAkey ${CA_KEY} -CAcreateserial -days ${SERVER_DAYS} -extensions v3_req -extfile ${SERVER_CONFIG_FILE}

rm ${SERVER_CSR}

cat ${SERVER_CRT} > ${SERVER_PEM}
cat ${SERVER_KEY} >> ${SERVER_PEM}

echo "===> Install to your server:"
echo " * ${SERVER_PEM}"
echo "   OR both ${SERVER_CRT} and ${SERVER_KEY}"
