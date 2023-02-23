import {ASNDERToList} from "./formatting-bytes";
import {encodeInteger, encodeSequence} from "./encode-element";

export class ReserveKeyRequest {
    keyLength: number
    cNonce: number
}

export class ReserveKeyResponse {
    errorId: number
    responseId: number
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
    let reserveKeyAndGetKeyHalf = 0x01;
    return encodeSequence([
        encodeInteger(reserveKeyAndGetKeyHalf),
        encodeInteger(request.keyLength),
        encodeInteger(request.cNonce)
    ]);
}


export function decodeReserveKeyResponse(encodedResponse): ReserveKeyResponse {
    let result = new ReserveKeyResponse();
    if(!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    result.errorId = data[0] as number;
    result.responseId = data[1] as number;
    result.cryptoNonce = data[2] as number;
    result.keyId = data[3] as Uint8Array;
    result.thisHalf = data[4] as Uint8Array;
    result.otherHash = data[5] as Uint8Array;
    result.hashAlgId = data[6] as Uint8Array;
    return result;
}
