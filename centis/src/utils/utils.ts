export function bytesToHexOctets(data) {
    let res = ""
    for (let i = 0; i < data.length; i++) {
        let tmp = data[i].toString(16)
        while (tmp.length < 2) {
            tmp = "0" + tmp;
        }
        res += tmp;
    }
    return res
}

export function bytesToSpacedHexOctets(data: Uint8Array): string {
    let res2 = ""
    for (let i = 0; i < data.length; i++) {
        if (i) res2 += " ";
        let tmp = data[i].toString(16)
        while (tmp.length < 2) {
            tmp = "0" + tmp;
        }
        res2 += tmp;
    }
    return res2
}

export function wsConnect(endpoint): Promise<WebSocket> {
    return new Promise((resolve, reject) => {
        const socket = new WebSocket(endpoint);
        socket.onopen = () => resolve(socket);
        socket.onerror = (error) => reject(error);
    });
}

export function wsSendRequest(socket, request) {
    return new Promise((resolve, reject) => {
        socket.onmessage = async (event) => {
            let data = new Uint8Array(await event.data.arrayBuffer());
            let response = new Uint8Array(data);
            resolve(response);
        };
        socket.onerror = (error) => reject(error);
        socket.send(request);
    });
}

export function ASNDERToList(seq): (number | Uint8Array)[] {
    let it = 2;
    let data = [];
    while (it < 2 + seq[1]) {
        if (seq[it] === 0x02) { // an integer follows
            it++;
            let int_len = seq[it++];
            let int = 0;
            for (let i = 0; i < int_len; i++) {
                int <<= 8;
                int += seq[it++];
            }
            data.push(int);
        } else if (seq[it] === 0x04 || seq[it] === 0x06) {
            it++;
            let arr_len = seq[it++];
            let res = new Uint8Array(arr_len);
            for (let i = 0; i < arr_len; i++) {
                res[i] = seq[it++];
            }
            data.push(res);
        } else {
            console.error("ERROR converting DER to list", seq[it]);
            return;
        }
    }
    return data;
}

export function hexOctetsToUint8Array(hexOctetStr: string): Uint8Array {
    let res = new Uint8Array(hexOctetStr.length / 2);
    for (let i = 0; i < res.length; i++) {
        res[i] = parseInt(hexOctetStr.substring(i * 2, i * 2 + 2), 16);

    }
    return res;
}

export interface RKAGHRequest {
    keyLength: number
    cNonce: number
}

export interface RKAGHResponse {
    cNonce: number
    errCode: number
    keyId: Uint8Array
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
    raw: Uint8Array
}

export function encodeRKAGHRequest(request: RKAGHRequest): [Uint8Array, Error] {
    if (request.keyLength !== 256) {
        return [null, new Error("Key length must be 256")]
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        return [null, new Error("Crypto nonce must be between 0 and 65535")]
    }

    const result = new Uint8Array(13)

    // sequence
    result[0] = 0x30;
    result[1] = 0x0B    // a sequence of 11 bytes will follow
    // reserveKeyAndGetKeyHalf request
    result[2] = 0x02;
    result[3] = 0x01    // an integer of 1 byte will follow
    result[4] = 0x01
    // requested key length (256)
    result[5] = 0x02;
    result[6] = 0x02    // an integer of 2 bytes will follow
    result[7] = 0x01;
    result[8] = 0x00
    // crypto nonce
    result[9] = 0x02;
    result[10] = 0x02   // an integer of 2 bytes will follow
    result[11] = request.cNonce >> 8;
    result[12] = request.cNonce % 256

    return [result, null];
}

export function parseRKAGHResponse(msg_arr): RKAGHResponse {
    let data = ASNDERToList(msg_arr);
    return {
        raw: msg_arr,
        cNonce: data[2] as number,
        errCode: data[0] as number,
        keyId: data[3] as Uint8Array,
        thisHalf: data[4] as Uint8Array,
        otherHash: data[5] as Uint8Array,
        hashAlgId: data[6] as Uint8Array,
    };
}

export function validateRKAGHRequest(request: RKAGHRequest): string {
    if (request.keyLength !== 256) {
        return "Key length must be 256"
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    return null;
}

export interface GKHRequest {
    keyLength: number
    cNonce: number
    keyId: string
}

export interface GKHResponse {
    cNonce: number
    errCode: number
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
}

export function encodeGKHRequest(request: GKHRequest): [Uint8Array, Error] {
    let error = validateGKHRequest(request)
    if (error) {
        return [null, new Error(error)]
    }

    let keyId = hexOctetsToUint8Array(request.keyId)
    const req = new Uint8Array(15 + keyId.length)
    // sequence
    req[0] = 0x30;
    req[1] = 13 + keyId.length    // a sequence of 13+len(keyId) bytes will follow

    // getKeyHalf request
    req[2] = 0x02;
    req[3] = 0x01    // an integer of 1 byte will follow
    req[4] = 0x02

    // key length
    req[5] = 0x02;
    req[6] = 0x02    // an integer of 2 bytes will follow
    req[7] = 0x01;
    req[8] = 0x00    // requested key length (256)

    // key id
    req[9] = 0x04;
    req[10] = keyId.length  // a byte array of keyId.length bytes will follow
    for (let i = 0; i < keyId.length; i++)
        req[i + 11] = keyId[i];

    // call#
    req[11 + keyId.length] = 0x02;
    req[12 + keyId.length] = 0x02   // an integer of 2 bytes will follow
    req[13 + keyId.length] = request.cNonce >> 8;
    req[14 + keyId.length] = request.cNonce % 256

    return [req, null];
}

export function parseGKHRequest(msg_arr): GKHResponse {
    let data = ASNDERToList(msg_arr);
    let result = {} as GKHResponse;
    result.cNonce = data[1] as number;
    result.errCode = data[2] as number;
    result.thisHalf = data[3] as Uint8Array;
    result.otherHash = data[4] as Uint8Array;
    result.hashAlgId = data[5] as Uint8Array;
    return result;
}

export function validateGKHRequest(request: GKHRequest) {
    if (request.keyLength !== 256) {
        return "Key length must be 256"
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    if (!/^[0-9A-F]+$/i.test(request.keyId)) {
        return "Key ID must be a hexadecimal string";
    }

    return null;
}

export interface GetStateRequest {
    cNonce: number
}

export interface GetStateResponse {
    cNonce: number
    errCode: number
    state: number
    keyId0: Uint8Array
    keyId1: Uint8Array
}

export function validateGetStateRequest(_: GetStateRequest) {
    return null;
}

export function encodeGetStateRequest(request: GetStateRequest): [Uint8Array, Error] {
    let error = validateGetStateRequest(request)
    if (error) {
        return [null, new Error(error)]
    }

    const req = new Uint8Array(9)
    // sequence
    req[0] = 0x30;
    req[1] = 7    // a sequence of 7 bytes will follow

    // getState request
    req[2] = 0x02;
    req[3] = 0x01    // an integer of 1 byte will follow
    req[4] = 0x03

    // call#
    req[5] = 0x02;
    req[6] = 0x02   // an integer of 2 bytes will follow
    req[7] = request.cNonce >> 8;
    req[8] = request.cNonce % 256

    return [req, null];
}

export function parseGetStateResponse(msg_arr): GetStateResponse {
    let data = ASNDERToList(msg_arr);
    let result = {} as GetStateResponse;
    result.errCode = data[1] as number;
    result.cNonce = data[2] as number;
    result.state = data[3] as number;
    result.keyId0 = data[4] as Uint8Array;
    result.keyId1 = data[5] as Uint8Array;
    return result;

}