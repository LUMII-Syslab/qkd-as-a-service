# QKD as a Service (QAAS)

Table of Contents:

1. [QAAS client API](#qaas-client-api)
  1. [0x01: `reserveKeyAndGetHalf` request](#0x01-reservekeyandgethalf-request)
  2. [0xff: `reserveKeyAndGetHalf` response](#0xff-reservekeyandgethalf-response)
  3. [0x02: `getKeyHalf` request](#0x02-getKeyHalf-request)
  4. [0xfe: `getKeyHalf` response](#0xfe-getKeyHalf-response)
2. [QAAS admin API](#qaas-admin-api)
  1. [0x03: `getState` request](#0x03-getstate-request)
  2. [0xfd: `getState` response](#0xfd-getstate-response)
  3. [0x04: `setState` request](#0x04-setstate-request)
  4. [0xfc: `setState` response](#0xfc-setstate-response)
3. [API Error codes & Other constants](#error-codes--other-constants)
4. [QAAS software structure & operation](#qaas-software-structure--operation)
  1. [Key Gathering](#key-gathering)
  2. [Handling `reserveKeyAndGetHalf` requests](#handling-reservekeyandgethalf-requests)
  3. [Handling `getKeyHalf` requests](#handling-getkeyhalf-requests)
  4. [KDC Synchronisation](#kdc-synchronisation)

## Starting the service & Configuration

Repository's layout:

```
.
├── centis
│   ├── node_modules
│   ├── package.json
│   ├── package-lock.json
│   ├── public
│   ├── README.md
│   ├── src
│   ├── tsconfig.json
│   └── yarn.lock
├── README.md
└── service
    ├── api
    ├── config.toml
    ├── configure.go
    ├── constants
    ├── gatherers
    ├── go.mod
    ├── go.sum
    ├── main.go
    ├── manager
    ├── models
    ├── scripts
    └── utils
```

The project consists of the QAAS itself (`service`) and the administration panel (`centis`).

To run the `service` go language has to be installed. Afterwards:

- cd into `service`

- run `go run .`

To run 'centis' npm has to be installed. Afterwards:

- cd into `centis`

- run `npm install`

- run `npm start`
  
## QAAS client API

### 0x01: `reserveKeyAndGetHalf` request

parameters:

1. endpoint id = `0x01`

2. key length

3. crypto nonce

encoded request example

```
30 0B 02 01 01 02 02 01 00 02 02 30 39
```

explanation:

`30` `0b`: sequence type (`0x30`) with length `0x0b` = 11 bytes;

`02` `01` `01`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x01` = 1; ( **endpoint id** )

`02` `02` `01 00`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x0100` = 256; ( **key length** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

### 0xff: `reserveKeyAndGetHalf` response

returns:

1. error code

2. response id = `0xff`

3. crypto nonce

4. key identifier

5. half of key bytes

6. hash(the other half)

7. hash algorithm id = `0x608648016503040211`

encoded return example:

```
30 23 02 01 00 02 01 ff 02 02 a4 55 04 04 28 8b de 07 04 02 21 a1 04 02 01 02 06 09 60 86 48 01 65 03 04 02 11
```

explanation:

`30` `23`: sequence type (`0x30`) with length `0x23` = 35 bytes;

`02` `01` `00`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x00` = 0; ( **error code** )

`02` `01` `ff`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0xff` = 255; ( **response id** )

`02` `02` `a4 55`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0xa455` = 42069; ( **crypto nonce** )

`04` `04` `28 8b de 07`: byte array (`0x04`) with length `0x04` = 4 bytes; ( **key identifier** )

`04` `02` `21 a1`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **half of key bytes** )

`04` `02` `01 02`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **hash(the other half)** )

`06` `09` `60 86 48 01 65 03 04 02 11`: object identifier (`0x06`) with length `0x09` = 9 bytes; ( **hash algorithm id** )

### 0x02: `getKeyHalf` request

1. endpoint id = `0x01`

2. key length

3. key identifier

4. crypto nonce

encoded request example

```
30 11 02 01 02 02 02 01 00 04 04 40 af a0 1f 02 02 30 39
```

explanation:

`30` `11`: sequence type (`0x30`) with length `0x11` = 17 bytes;

`02` `01` `02`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x02` = 2; ( **endpoint id** )

`02` `02` `01 00`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x0100` = 256; ( **key length** )

`04` `04` `40 af a0 1f`: byte array (`0x04`) with length `0x04` = 4 bytes; ( **key identifier** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )


### 0xfe: `getKeyHalf` response

returns:

1. error code

2. response id = `0xff`

3. crypto nonce

4. half of key bytes

5. hash(the other half)

6. hash algorithm id = `0x608648016503040211`

encoded response example:

```
30 1d 02 01 00 02 01 fe 02 02 30 3a 04 02 e1 5c 04 02 01 02 06 09 60 86 48 01 65 03 04 02 11
```

explanation:

`30` `1d`: sequence type (`0x30`) with length `0x1d` = 29 bytes;

`02` `01` `00`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x00` = 0; ( **error code** )

`02` `01` `fe`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0xfe` = 254; ( **response id** )

`02` `02` `30 3a`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x303a` = 12346; ( **crypto nonce** )

`04` `02` `e1 5c`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **half of key bytes** )

`04` `02` `01 02`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **hash(the other half)** )

`06` `09` `60 86 48 01 65 03 04 02 11`: object identifier (`0x06`) with length `0x09` = 9 bytes; ( **hash algorithm id** )

## QAAS admin API

- `getState` is used to determine the state and key identifiers of the first even and odd parity keys respectively.

- `setState` is used to set the state of kdc and synchronize keys.

### 0x03: `getState` request

parameters:

1. endpoint id = `0x03`

2. crypto nonce

encoded request example:

```
30 07 02 01 03 02 02 30 39
```

explanation:

`30` `07`: sequence type (`0x30`) with length `0x07` = 7 bytes;

`02` `01` `03`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x03` = 3; ( **endpoint id** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

### 0xfd: `getState` response

returns:

1. error code

2. response id = `0xfd`

3. crypto nonce

4. kdc state id
	
	- `EMPTY` = 0		( when there are no keys received from QKD device )
	- `RECEIVING` = 1	( when at least one key has been received )
	- `RUNNING` = 2		( when keys can be reserved by the users )

5. even key id

6. odd key id


TODO

### 0x04: `setState` request

TODO

### 0x05:`getStatistics` request

TODO

## Error codes & Other constants

error codes: 
- NoError          = 0
- ErrorKeyNotFound = 1
- ErrorNotRunning  = 2
- ErrorInternal    = 3
- ErrorInvalidReq  = 4

## QAAS software structure & operation

TODO

### Key Gathering

TODO

### Handling `reserveKeyAndGetHalf` requests

TODO

### Handling `getKeyHalf` requests

TODO

### KDC Synchronisation

TODO
