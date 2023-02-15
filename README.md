# QKD as a Service (QaaS)

Quantum key distribution (QKD) is a key agreement method that relies on the laws of physics and ensures that the keys have not been eavesdropped on or modified by a third party.

QKD equipment (such as IDQ Clavis or Cerberis) is expensive, requires specific infrastructure (2-3 optical fiber links), and has high operation costs.

QKD as a Service, QaaS, allows the users to obtain a shared secret from two *remote* QKD devices in a secure way. QaaS uses a mix of quantum and classical channels in a way that is sustainable for active attacks on any single communication segment.

This project consists of the QaaS itself (`service`) and the administration panel (`centis`). The code from this project is intended to be launched on the servers with directly attached QKD devices (however, we provide two types of QKD device simulators).

End users wishing to use QaaS, should rely on our [QKD user library](https://github.com/LUMII-Syslab/qkd-user-lib).

Please, cite our paper: [the link will be provided later in 2023]

## Prerequisites

* git client

* Golang compiler

* npm (for the administration panel)

In order to install all prerequisites on Ubuntu, run:

```bash
sudo apt install git golang-go npm
```

In order to install prerequisites on macOS, run:

```bash
brew install golang npm
```

## Clone and Install

```bash
git clone https://github.com/LUMII-Syslab/qkd-as-a-service.git
cd qkd-as-a-service/service
go mod tidy
```

For the administration panel:

```bash
cd ../centis
npm install
```



## Starting the service

From the `service` directory, launch:

```bash
go run .
```

## Accessing the administration panel

From the `centis` directory, launch:

```bash
npm start
```

The browser pointing to `http://localhost:3000/qkdc-service` should be opened automatically.

## Internal APIs

Docs on the internal APIs used between QaaS users and servers can be found [here](https://github.com/LUMII-Syslab/qkd-as-a-service/blob/master/API.md).
