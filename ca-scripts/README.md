# Certification Authority (CA) Scripts

by Sergejs Kozlovičs, 2022-2024

## Prerequisites

1. Install openssl. If you need openssl with PQC algorithms, you can use [our scripts on GitHub](https://github.com/LUMII-Syslab/oqs-haproxy). The installation can be done either in a native Linux/Unix environment or via Cygwin on Windows. By default, it is assumed that your openssl is launchable as  `openssl` or `/usr/local/bin/openssl`. If your openssl is somewhere else, edit the `OQS_OPENSSL` variable and (optionally) `OQS_OPENSSL_CA_REQ_ARGS`,  `OQS_OPENSSL_CLIENT_REQ_ARGS`, and `OQS_OPENSSL_SERVER_REQ_ARGS` (for specifying additional req arguments) in `_vars.sh`.
2. Optionally, install JDK16+ (OpenJDK or GraalVM are OK). We need its `keytool` to be launchable from the command line. The tool is used to create a Java trust store for the CA certificate. We need at least JDK v16+ since it supports some latest hash algorithms that are used in recent file format of the key/trust store.

## The scripts

* `ca_init.sh` generates a CA root key pair and creates the corresponding self-signed CA certificate. The CA key will be used to sign server and client certificates (the default expiration time is set to 10 years).
  * **Deploy** the generated `ca.truststore` file when Java trust store file is needed.
  
  * **Deploy** the generated `ca.crt` file when a PEM file is needed. For example, this file can be used to  configure HAProxy to authenticate clients signed by our CA.
* `ca_renew.sh` re-generates the CA root key pair and its self-signed CA certificate. This script has to be called when the previous CA key pair is about to expire.
* `new_server_key.sh` generates and signs (by our CA) a server certificate.  The first three arguments specify:
  * the CA name,
  * the server name (no spaces or special symbols, please!), which is the subdirectory name, where we will put the generated certs/keys;
  * the openssl configuration file (e.g., `server.cnf` ).
> **Deploy** the generated `server.pem` file to your server/proxy. That file contains both the server private key and the signed certificate. Don't forget to restart the server/proxy.

* `new_client_key.sh` generates and signs (by our CA) a client certificate. Each user should have their own client key and certificate. The first three arguments specify:
  * the CA name,
  * the user name (no spaces or special symbols, please!),  which is the subdirectory name, where we will put the generated certs/keys;
  * the openssl configuration file (e.g., `client.cnf` ).
> **Deploy** the `token.keystore` file (containing the client private key and its signed certificate) when a Java key store file is needed.
> **Deploy** the `client.key` and `client.crt` files (containing the client private key and its signed certificate) when PEM files are needed.

* `sign_client_csr.sh` signs a CSR (certificate signing request) provided by a 3rd party for signing client certificates, when we do not own the private key.  The first two arguments specify:
  * the CA name,
  * the CSR file (usually, with the .csr or .pem extension).

> **Deploy** the corresponding generated .crt file to your client.
