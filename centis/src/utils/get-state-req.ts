import { ASNDERToList } from "./formatting-bytes";
import {encodeInteger, encodeSequence} from "./encode-element";

export class GetStateRequest {
    cNonce: number
}

export class GetStateResponse {
    errorId: number
    responseId: number
    cryptoNonce: number
    stateId: number
    keysStored: number
    reservable: number
    keysServed: number
    keysAdded: number
    oldestEvenKeyId: Uint8Array
    oldestOddKeyId: Uint8Array
}

export function validateGetStateRequest(request: GetStateRequest): string {
    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    return null;
}

export function encodeGetStateRequest(request: GetStateRequest): Uint8Array {
    let getStateRequest = 0x03;
    return encodeSequence([
        encodeInteger(getStateRequest),
        encodeInteger(request.cNonce)
    ]);
}


export function decodeGetStateResponse(encodedResponse): GetStateResponse {
    let result = new GetStateResponse();
    if(!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    console.log(data);
    result.errorId = data[0] as number;
    result.responseId = data[1] as number;
    result.cryptoNonce = data[2] as number;
    result.stateId = data[3] as number;
    result.keysStored = data[4] as number;
    result.reservable = data[5] as number;
    result.keysServed = data[6] as number;
    result.keysAdded = data[7] as number;
    result.oldestEvenKeyId = data[8] as Uint8Array;
    result.oldestOddKeyId = data[9] as Uint8Array;
    return result;
}
