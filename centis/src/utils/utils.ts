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
    // if data is null return null
    if (data === null) {
        return "";
    }
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



export function ASNDERToList(seq): (number | Uint8Array)[] {
    try {
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
    } catch (e) {
        console.error("ERROR converting DER to list", e);
        return;
    }
}

export function hexOctetsToUint8Array(hexOctetStr: string): Uint8Array {
    let res = new Uint8Array(hexOctetStr.length / 2);
    for (let i = 0; i < res.length; i++) {
        res[i] = parseInt(hexOctetStr.substring(i * 2, i * 2 + 2), 16);

    }
    return res;
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
    raw: Uint8Array
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
    result.raw = msg_arr
    result.cNonce = data[2] as number;
    result.errCode = data[0] as number;
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