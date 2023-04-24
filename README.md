# QKD as a Service (QaaS)

Quantum key distribution (QKD) is a key agreement method that relies on the laws of physics and ensures that the keys have not been eavesdropped on or modified by a third party.

QKD equipment (such as IDQ Clavis or Cerberis) is expensive, requires specific infrastructure (2-3 optical fiber links), and has high operational costs.

QKD as a Service, QaaS, allows the users to obtain a shared secret from two *remote* QKD devices in a secure way. QaaS uses a mix of quantum and classical channels in a way that is sustainable for active attacks on any single communication segment.

This project consists of

* the QaaS itself (`service`),
* the administration panel (`centis`),
* the QaaS user library, and
* post-quantum proxy, PQProxy, which can be used to enable quantum-safe (PQC) connections between nodes.

> The QaaS user library and PQProxy share the same Java code base inside `userlib+pqproxy`.

# The Names of the Nodes

Following IDQ Clavis notation, we call QKD devices Alice (the transmitter) and Bob (the receiver).

The QaaS servers, which are directly attached to Alice and Bob, are called Aija and Brencis. They can be accessed from the controlling module called Centis.

The users who wish to establish a secure TLS connection based on a shared key received from QaaS are called User 1 and User 2.

Since not all nodes have out-of-the-box TLS support (in some cases we need TLS with PQC), we introduce additional proxies.

The following figure illustrates all the nodes that participate in the QaaS system:

![qkd-infrastructure](qkd-infrastructure.png)

The QaaS infrastructure ensures secure (green) link between User 1 and User 2 by utilizing the optical fiber QKD system (in the bottom) and multiple classical PQC links.

# Keys and Certificates

Multiple PQC and ECC keys and certificates have to be generated in order for the whole QaaS system could operate in a secure way. We use web sockets in our protocols, meaning that keys and certificates ensure the secure web sockets protocol over TLS (`wss`) instead of plain web sockets (`ws`), like `https` is a secure version of `http`.

In order to be able to generate keys and sign certificates, OpenSSL with post-quantum algorithms (from LibOQS) has to be installed. For OpenSSL 1.1.1, use our scripts from https://github.com/LUMII-Syslab/oqs-haproxy. For OpenSSL 3, install oqs-provider from https://github.com/open-quantum-safe/oqs-provider.

Then run:

```bash
scripts/gen-keys.sh
```

All the keys (for CA, Aija, Brencis, Centis, users, and proxies) are placed into subfolders within `ca-scripts`. The keys are mostly used by the scripts (from the `scripts` directory). Some (ECC, non-PQC) keys have to be installed in a non-PQC browser, where Centis will be launched (see below).

# Running QaaS (Aija and Brencis)

The code from the `service` project is intended to be launched on the servers with directly attached QKD devices  (however, we provide two types of QKD device simulators for experiments and tests; see below how to configure them).

## Prerequisites

In order to install all prerequisites (Golang compiler) on Ubuntu, run:

```bash
sudo apt install golang-go
```

In order to install prerequisites on macOS, run:

```bash
brew install golang
```

From the `service` directory, launch:

```bash
cd service
go mod tidy
```

## Launch

Edit `config.toml` to configure the QaaS service. Typically, the service serves one endpoint - either Aija or Brencis. However, in the experimental environment, both can be launched at once.

Aija and Brencis are launched as non-secure web socket servers (upgraded HTTP). Specify `aija_port` (recommended 8001), `brencis_port` (recommended 8002), or both. Besides, specify the `gatherer`, which will be used to collect keys. Possible values are:

* `clavis` - uses the real IDQ Clavis device to collect quantumly distributed keys; in this case, also specify the `clavis_url` property;
* `pseudorandom` - generates keys randomly (both Aija and Brencis must be launched from the same process, i.e., both `aija_port` and `brencis_port` have to be specified);
* `filesystem` - takes keys from the file system folder (the folder can be shared); specify the folder path in the `fs_gatherer_dir` property. File names must be in the format `<key-ID>.key` (e.g., `00f2b5c1-0284-4fc9-8d7d-d663f31c7d7c.key`) and the content must be the 256-bit binary key (32 bytes long).

We have prepared a script for launching 4 reverse proxies (2 in front of Aija and 2 in front of Brencis) to enforce PQC connections from Users 1 and 2 and from Centis (Centis has separate proxies since it can control Aija and Brencis; Centis has a specific client key+certificate).

In order to launch **Aija and Brencis in the PQC mode** (i.e., with all 4 proxies), run:

```bash
../scripts/aija+brencis.sh
```

The script launches several copies of the default terminal app for the OS (`gnome-terminal` on Linux, `Terminal` on macOS, and  `cmd` with Cygwin on Windows).

> In order to launch **Aija and Brencis in the non-secure** web socket **mode** (without proxies), run:
> ```bash
> go run .
> ```


# Running the Administration Panel (Centis)

## Prerequisites

In order to install all prerequisites (npm) on Ubuntu, run:

```bash
sudo apt install npm
```

In order to install prerequisites on macOS, run:

```bash
brew install npm
```

Then from the `centis` directory, launch:
```bash
npm install
```

## Launch

From the `scripts` directory, launch:

```bash
../centis.sh
```

The script launches 2 local HTTP-to-PQC/HTTPS proxies. Besides, the script launches the Node.js backend for Centis. The browser pointing to `http://localhost:3000/qkdc-service` should be opened automatically.

Since two proxies are launched locally, you can use `ws://localhost:8080/ws` and `ws://localhost:8081/ws` as addresses for Aija and Brencis.

> If you would like to run Centis without proxies, from the `centis` directory, launch:
> ```bash
> npm start
> ```
> Notice that in you will need a PQC-enabled browser built with LibOQS.



## Centis Paranoic Mode: HTTPS in a non-PQC browser

If you are paranoic, it is possible to launch Centis in HTTPS mode even if your browser does not support PQC. For that, the `scripts/gen-keys.sh` script has also generated non-PQC elliptic curve (ECC) keys and certificates that have to be installed in your non-PQC browser:

* Install `ca_scripts/proxy-client-centis/proxy-client-centis.pem` as a client key to be used to authenticate in web sites.
* Install `ca_scripts/proxy-ca/ca.crt` as a trusted CA to identify websites.

Then edit `scripts/pqproxy-local2pqccentis4aija.properties` and `pqproxy-local2pqccentis4brencis.properties` and specify `sourceTls=true`. Ensure the correct source and target key stores and trust stores are also specified there.

# User Library Usage Example (Users 1 and 2)

Currently, we provide Java source code for sample implementations of User 1 (the TLS client) and User 2 (the TLS server). The symmetric TLS key is obtained from Aija and Brencis by means of the Butterfly Protocol from our paper.

Source files are:

* `userlib+pqproxy/src/test/java/lv/lumii/test/QkdTestUser1.java` and
* `userlib+pqproxy/src/test/java/lv/lumii/test/QkdTestUser2.java`.

Before launching User 1 and User 2, edit the `userlib+pqproxy/qkd.properties` file, where the client (User 1) and server (User 2) PQC credentials are specified, as well as Aija and Brencis URLs.

User 2 (the server) can be launched via:

```bash
userlib+pqproxy/scripts/user2.sh
```

Start User 2 (the server) before User 1 so User 1 does not receive the "connection refused" error.

User 1 can be launched via:

```bash
userlib+pqproxy/scripts/user1.sh
```

# Docs on Internal APIs

Docs on the internal APIs used between QaaS users and servers can be found at [https://github.com/LUMII-Syslab/qkd-as-a-service/blob/master/API.md](https://github.com/LUMII-Syslab/qkd-as-a-service/blob/master/API.md).
