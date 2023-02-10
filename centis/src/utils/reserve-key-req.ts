import {ASNDERToList} from "./formatting-bytes";

export class ReserveKeyRequest {
    keyLength: number
    cNonce: number
}

export class ReserveKeyResponse {
    errCode: number
    cryptoNonce: number
    keyId: Uint8Array
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
}

export function validateReserveKeyRequest(request: ReserveKeyRequest): string {
    if (request.keyLength !== 256) {
        return "Key length must be 256"
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    return null;
}

export function encodeReserveKeyRequest(request: ReserveKeyRequest): Uint8Array {
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
    return result;
}


export function decodeReserveKeyResponse(encodedResponse): ReserveKeyResponse {
    let result = new ReserveKeyResponse();
    if(!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    result.errCode = data[0] as number;
    result.cryptoNonce = data[2] as number;
    result.keyId = data[3] as Uint8Array;
    result.thisHalf = data[4] as Uint8Array;
    result.otherHash = data[5] as Uint8Array;
    result.hashAlgId = data[6] as Uint8Array;
    return result;
}
