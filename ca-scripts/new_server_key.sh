#!/bin/bash
#
# Generates a new server key (to be used within, e.g., HAProxy, Apache, or nginx).
# Script arguments: ca-name openssl-config-file server-certificate-validity-in-days new-server-keypair.pem
#
# Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
# Licence: MIT
# Contributors:
#   Sergejs Kozlovics, 2022-2023

export PATH=/usr/bin:$PATH
export DIR=$(dirname $0)

if [ "$4" = "" ]; then
  echo Usage: $0 ca-name openssl-server-config-file server-certificate-validity-in-days new-server-keypair.pem
  echo "  (If you are using our server.cnf as a template for openssl-config-file, please, edit it"
  echo "   to reflect your organization name, IP, and domain name!)"
  exit
fi

export CA_NAME=$1
export SERVER_CONFIG_FILE=$2
export DAYS=$3
export SERVER_KEYPAIR_FILE=$4
source $DIR/_vars.sh
source $CA_VARS

${OQS_OPENSSL} req -new -newkey ${SIG_ALG} -keyout ${DIR}/server.key -out ${DIR}/server.csr -nodes -config ${SERVER_CONFIG_FILE} ${OQS_OPENSSL_REQ_ARGS}
${OQS_OPENSSL} x509 -req -in server.csr -out ${DIR}/server.crt -CA ${CA_CRT} -CAkey ${CA_KEY} -CAcreateserial -days ${DAYS} -extensions v3_req -extfile ${SERVER_CONFIG_FILE}

rm ${DIR}/server.csr

cat ${DIR}/server.crt > ${DIR}/server.pem
cat ${DIR}/server.key >> ${DIR}/server.pem

echo "Moving (installing) ${DIR}/server.pem as ${SERVER_KEYPAIR_FILE}..."
mkdir -p `dirname $SERVER_KEYPAIR_FILE`
mv ${DIR}/server.pem $SERVER_KEYPAIR_FILE
rm ${DIR}/server.crt
rm ${DIR}/server.key
