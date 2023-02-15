# Certification Authority (CA) Scripts

by Sergejs Kozlovičs, 2022-2023

## Prerequisites

1. Install openssl. If you need openssl with PQC algorithms, you can use [our scripts on GitHub](https://github.com/LUMII-Syslab/oqs-haproxy). The installation can be done either in a native Linux/Unix environment or via Cygwin on Windows. By default, it is assumed that your openssl is located at `/opt/oqs/bin/openssl`. If your openssl is somewhere else, edit the `OQS_OPENSSL` variable and (optionally) the `OQS_OPENSSL_REQ_ARGS` (e.g., for specifying the config file for new keys) in `_vars.sh`.
2. JDK16+ (OpenJDK or GraalVM are OK) with paths to its binaries in the `PATH` environment variable. We will need JDK `keytool` to create Java key store and trust store. We need JDK v16+ since it supports some latest hash algorithms that are used in recent file format of the key/trust store.

## The scripts

* `ca_init.sh` generates a CA root key pair and creates the corresponding self-signed CA certificate. The CA key will be used to sign server and client certificates (the default expiration time is set to 10 years).
  * **Deploy** the generated `ca.truststore` file when Java trust store file is needed.
  
  * **Deploy** the generated `ca.crt` file when a PEM file is needed. For example, this file can be used to  configure HAProxy to authenticate clients signed by our CA.
  
* `ca_renew.sh` re-generates the CA root key pair and its self-signed CA certificate. This script has to be called when the previous CA key pair is about to expire.

* `new_server_key.sh` generates and signs (by our CA) a server certificate.  The first four arguments specify the CA name, the openssl configuration file (e.g., `server.cnf` ), the certificate expiration time (in days), and the destination .pem file to deploy/install at your web server or proxy.
  * The destination .pem file will contain both the server private key and the signed certificate. Configure your server/proxy to use that file and restart the server/proxy.
  
* `new_client_key.sh` generates and signs (by our CA) a client certificate. The first three arguments specify the CA name, the user name (no spaces or special symbols, please!) and the certificate expiration time (in days). Each user should have their own client certificate.
  * **Deploy** the `token.keystore` file (containing the client private key and its signed certificate) when a Java key store file is needed.
  * **Deploy** the `client.key` and `client.crt` files (containing the client private key and its signed certificate) when PEM files are needed.

