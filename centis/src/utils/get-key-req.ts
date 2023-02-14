import {ASNDERToList, hexOctetsToUint8Array} from "./formatting-bytes";

export class GetKeyRequest {
    keyLength: number
    keyId: string
    cNonce: number
}

export class GetKeyResponse {
    errCode: number
    responseId: number
    cryptoNonce: number
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
}

export function validateGetKeyRequest(request: GetKeyRequest): string {
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

export function encodeGetKeyRequest(request: GetKeyRequest): Uint8Array {
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

    return req;
}


export function decodeGetKeyResponse(encodedResponse): GetKeyResponse {
    let result = new GetKeyResponse();
    if(!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    result.errCode = data[0] as number;
    result.responseId = data[1] as number;
    result.cryptoNonce = data[2] as number;
    result.thisHalf = data[3] as Uint8Array;
    result.otherHash = data[4] as Uint8Array;
    result.hashAlgId = data[5] as Uint8Array;
    return result;
}
