import { ASNDERToList } from "./formatting-bytes";

export class GetStateRequest {
    cNonce: number
}

export class GetStateResponse {
    errorId: number
    responseId: number
    cryptoNonce: number
    stateId: number
    evenKeyId: Uint8Array
    oddKeyId: Uint8Array
}

export function validateGetStateRequest(request: GetStateRequest): string {
    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    return null;
}

export function encodeGetStateRequest(request: GetStateRequest): Uint8Array {
    const result = new Uint8Array(9)
    // sequence
    result[0] = 0x30;
    result[1] = 0x07    // a sequence of 7 bytes will follow
    // reserveKeyAndGetKeyHalf request
    result[2] = 0x02;
    result[3] = 0x01    // an integer of 1 byte will follow
    result[4] = 0x03
    // crypto nonce
    result[5] = 0x02;
    result[6] = 0x02   // an integer of 2 bytes will follow
    result[7] = request.cNonce >> 8;
    result[8] = request.cNonce % 256
    return result;
}


export function decodeGetStateResponse(encodedResponse): GetStateResponse {
    let result = new GetStateResponse();
    if(!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    result.errorId = data[0] as number;
    result.responseId = data[1] as number;
    result.cryptoNonce = data[2] as number;
    result.stateId = data[3] as number;
    result.evenKeyId = data[4] as Uint8Array;
    result.oddKeyId = data[5] as Uint8Array;
    return result;
}
