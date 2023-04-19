# QKD as a Service (QAAS) API

## Table of Contents:

- [QKD as a Service (QAAS) API](#qkd-as-a-service-qaas-api)
  - [Table of Contents:](#table-of-contents)
  - [Introduction](#introduction)
  - [QAAS client API](#qaas-client-api)
    - [0x01: `reserveKeyAndGetHalf` request](#0x01-reservekeyandgethalf-request)
    - [0xff: `reserveKeyAndGetHalf` response](#0xff-reservekeyandgethalf-response)
    - [0x02: `getKeyHalf` request](#0x02-getkeyhalf-request)
    - [0xfe: `getKeyHalf` response](#0xfe-getkeyhalf-response)
  - [QAAS admin API](#qaas-admin-api)
    - [0x03: `getState` request](#0x03-getstate-request)
    - [0xfd: `getState` response](#0xfd-getstate-response)
    - [0x04: `setState` request](#0x04-setstate-request)
    - [0x05:`getStatistics` request](#0x05getstatistics-request)
  - [Error codes \& Other constants](#error-codes--other-constants)
  - [QAAS software structure \& operation](#qaas-software-structure--operation)
    - [Key Gathering](#key-gathering)
    - [Handling `reserveKeyAndGetHalf` requests](#handling-reservekeyandgethalf-requests)
    - [Handling `getKeyHalf` requests](#handling-getkeyhalf-requests)
    - [KDC Synchronisation](#kdc-synchronisation)

## Introduction

Every request and response is an ASN.1 DER encoded sequence of elements.
Each element of the sequence consists of its `type`, `length` and `value`.
The types and their respective encodings used in QAAS requests are:


| type              | encoding  |
|-------------------|------|
| SEQUENCE OF       | 0X30 |
| INTEGER           | 0X02 |
| OCTET ARRAY       | 0X04 |
| OBJECT IDENTIFIER | 0X06 |


## QAAS client API

- endpoint `reserveKeyAndGetHalf` reserves a new key from a KDC.

- endpoint `getKeyHalf` fetches a reserved key from the other KDC.

### 0x01: `reserveKeyAndGetHalf` request

| ordinal |       parameter      |   type  |               description & notes               |
|:-------:|----------------------|---------|-------------------------------------------------|
|    1    | endpoint id = `0x01` | integer | Specifies the `reserveKeyAndGetHalf` request.   |
|    2    | key length = `256`   | integer | Currently only 256 byte key fetching is supported. |
|    3    | crypto nonce         | integer | Value should be between 0 and 2^63-1.           |

<details>
<summary>encoded request example</summary>

```
30 0B 02 01 01 02 02 01 00 02 02 30 39
```

explanation:

`30` `0b`: sequence type (`0x30`) with length `0x0b` = 11 bytes;

`02` `01` `01`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x01` = 1; ( **endpoint id** )

`02` `02` `01 00`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x0100` = 256; ( **key length** in bits )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

</details>

### 0xff: `reserveKeyAndGetHalf` response

| ordinal | parameter | type | description & notes |
|:-------:|-----------|------|---------------------|
| 1 | error code | integer | |
| 2 | response id | integer | |
| 3 | key identifier | octet array | |
| 4 | key bytes first half | octet array | |
| 5 | other byte half hash | octet array | |
| 6 | hash algorithm id | object identifier | |


returns an ASN1Sequence with these items:

0. error code: ASN integer

1. response id: ASN integer = `0xFF` (meaning: `reserveKeyAndGetKeyHalf` response)

2. crypto nonce: ASN integer

3. key identifier: ASN octet string

4. half of key bytes: ASN octet string

5. hash(the other half of the key): ASN octet string

6. hash algorithm id: ASN Object Identifier, e.g., encoded as `0x608648016503040211`

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

`06` `09` `60 86 48 01 65 03 04 02 11`: object identifier (`0x06`) with length `0x09` = 9 bytes; ( **hash algorithm id** ); for SHAKE-128 it is `60 86 48 01 65 03 04 02 11`, which corresponds to 2.16.840.1.101.3.4.2.11

### 0x02: `getKeyHalf` request

0. endpoint id = `0x02` (function ID for `getKeyHalf)

1. key length

2. key identifier

3. crypto nonce

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

| ordinal | value                  | type        | description & notes                            |
|:-------:|------------------------|-------------|------------------------------------------------|
|    1    | error code             | integer     |                                                |
|    2    | response id = `0xfe`   | integer     | Specifies the `getKeyHalf` response. |
|    3    | crypto nonce           | integer     |                                                |
|    4    | half of key bytes      | octet array |                                                |
|    5    | hash of the other half | octet array |                                                |
|    6    | hash algorithm id      | object id   |                                                |

0. error code

1. response id = `0xFE`

2. crypto nonce

3. half of key bytes

4. hash(the other half)

5. hash algorithm id = `0x608648016503040211`

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

| ordinal | parameter           | type    | description & notes                   |
|:-------:|---------------------|---------|---------------------------------------|
|    1    | endoint id = `0x03` | integer |                                       |
|    2    | crypto nonce        | integer | Value should be between 0 and 2^63-1. |

<details>
<summary>encoded request example:</summary>
<div>

```
30 07 02 01 03 02 02 30 39
```

explanation:

`30` `07`: sequence type (`0x30`) with length `0x07` = 7 bytes;

`02` `01` `03`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x03` = 3; ( **endpoint id** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

</details>

### 0xfd: `getState` response

| ordinal | value                | type        | description & notes                |
|:-------:|----------------------|-------------|------------------------------------|
|    1    | error code           | integer     |                                    |
|    2    | response id = `0xfd` | integer     | Specifies the `getState` response. |
|    3    | crypto nonce         | integer     |                                    |
|    4    | KDC state id         | integer     |                                    |
|    5    | oldest even key id   | octet array |                                    |
|    6    | oldest odd key id    | octet array |                                    |

<details>
<summary>encoded response example</summary>
TODO
</details>

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
