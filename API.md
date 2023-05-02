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
| 0 | endpoint id = `0x01`| integer | Specifies the `reserveKeyAndGetHalf` request.   |
| 1 | key length = `256`  | integer | Currently only 256 byte key fetching is supported. |
| 2 | crypto nonce        | integer | Value should be between 0 and 2^63-1.           |

<details>
<summary>encoded request example</summary>

```
30 0b 02 01 01 02 02 01 00 02 02 30 39
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
| 0 | error code | integer | |
| 1 | response id = `0xff` | integer | Specifies `reserveKeyAndHalf` response. 0xff denotes -1. |
| 2 | crypto nonce | integer | |
| 3 | key identifier | octet array | |
| 4 | key bytes first half | octet array | |
| 5 | other byte half hash | octet array | |
| 6 | hash algorithm id | object id | |

<details>

<summary>encoded response example:</summary>

```
30 5b 02 01 00 02 01 ff 02 02 30 3a 04 20 f0 ee 71 0b dd 47 79 3c b9 f6 a5 f1 af f4 2b 4d a4 24 28 6a 5c eb a2 91 f3 9e f1 16 96 57 af 14 04 10 8d cf be 25 0f 1c 06 6d 93 21 0d d0 2d a3 c4 d5 04 10 3d 53 11 a5 37 ba 6f 66 d2 ce 29 11 46 bb c1 ca 06 09 60 86 48 01 65 03 04 02 11
```

```
SEQUENCE (7 elem)
  INTEGER 0
  INTEGER -1
  INTEGER 12346
  OCTET ARRAY (32 byte) F0EE710BDD47793CB9F6A5F1AFF42B4DA424286A5CEBA291F39EF1169657AF14
  OCTET ARRAY (16 byte) 8DCFBE250F1C066D93210DD02DA3C4D5
  OCTET ARRAY (16 byte) 3D5311A537BA6F66D2CE291146BBC1CA
  OBJECT IDENTIFIER 2.16.840.1.101.3.4.2.17 shake128len (NIST Algorithm)
```

### 0x02: `getKeyHalf` request

| ordinal | parameter | type | description & notes |
|:-------:|-----------|------|---------------------|
| 0 | endpoint id = `0x02` | integer | Specifies the `getKeyHalf` request. |
| 1 | key length = `256` | integer | Currently only 256 byte key fetching is supported. |
| 2 | key identifier | octet array | |
| 3 | crypto nonce | integer | Value should be between 0 and 2^63-1. |

<details>

<summary>encoded request example</summary>

```
30 11 02 01 02 02 02 01 00 04 04 40 af a0 1f 02 02 30 39
```

explanation:

`30` `11`: sequence type (`0x30`) with length `0x11` = 17 bytes;

`02` `01` `02`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x02` = 2; ( **endpoint id** )

`02` `02` `01 00`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x0100` = 256; ( **key length** )

`04` `04` `40 af a0 1f`: byte array (`0x04`) with length `0x04` = 4 bytes; ( **key identifier** )

`02` `02` `30 39`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x3039` = 12345; ( **crypto nonce** )

</details>

### 0xfe: `getKeyHalf` response

| ordinal | value                  | type        | description & notes                            |
|:-------:|------------------------|-------------|------------------------------------------------|
|    0    | error code             | integer     |                                                |
|    1    | response id = `0xfe`   | integer     | Specifies the `getKeyHalf` response. 0xfe denotes -2. |
|    2    | crypto nonce           | integer     |                                                |
|    3    | key bytes second half | octet array |                                                |
|    4    | other byte half hash | octet array |    |
|    5    | hash algorithm id      | object id   |  |


<details>

<summary>encoded response example:</summary>

```
30 39 02 01 00 02 01 fe 02 02 30 3a 04 10 85 0e 6c 1a 4f ac 51 da 8d d3 03 7a 77 ad c4 e0 04 10 73 37 44 1b 7d d3 93 64 c7 80 df db 2b 09 bd 60 06 09 60 86 48 01 65 03 04 02 11
```

explanation:

`30` `1d`: sequence type (`0x30`) with length `0x1d` = 29 bytes;

`02` `01` `00`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0x00` = 0; ( **error code** )

`02` `01` `fe`: integer type (`0x02`) with length `0x01` = 1 bytes, value: `0xfe` = -2; ( **response id** )

`02` `02` `30 3a`: integer type (`0x02`) with length `0x02` = 2 bytes, value: `0x303a` = 12346; ( **crypto nonce** )

`04` `02` `e1 5c`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **half of key bytes** )

`04` `02` `01 02`: byte array (`0x04`) with length `0x02` = 2 bytes; ( **hash(the other half)** )

`06` `09` `60 86 48 01 65 03 04 02 11`: object identifier (`0x06`) with length `0x09` = 9 bytes; ( **hash algorithm id** )

</details>

## QAAS admin API

- `getState` is used to determine the state and key identifiers of the first even and odd parity keys respectively.

- `setState` is used to set the state of kdc and synchronize keys.

### 0x03: `getState` request

| ordinal | parameter           | type    | description & notes                   |
|:-------:|---------------------|---------|---------------------------------------|
|    0    | endoint id = `0x03` | integer |                                       |
|    1    | crypto nonce        | integer | Value should be between 0 and 2^63-1. |

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
|    0    | error code           | integer     |                                    |
|    1    | response id = `0xfd` | integer     | Specifies the `getState` response. |
|    2    | crypto nonce         | integer     |                                    |
|    3    | KDC state id         | integer     |                                    |
|    4    | oldest even key id   | octet array |                                    |
|    5    | oldest odd key id    | octet array |                                    |

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
