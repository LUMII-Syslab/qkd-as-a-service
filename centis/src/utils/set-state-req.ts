import {ASNDERToList, hexOctetsToUint8Array} from "./formatting-bytes";
import {encodeByteArray, encodeInteger, encodeSequence} from "./encode-element";

export class SetStateRequest {
    stateId: number
    evenKeyId: string
    oddKeyId: string
    cNonce: number
}

export class SetStateResponse {
    errorId: number
    responseId: number
    cryptoNonce: number
}

export function validateSetStateRequest(request: SetStateRequest): string {
    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    if (request.stateId !== 0 && request.stateId !== 2) {
        return "State ID must be 0 or 2";
    }

    if (!/^[0-9A-F]+$/i.test(request.evenKeyId)) {
        return "Key ID must be a hexadecimal string";
    }

    if (!/^[0-9A-F]+$/i.test(request.oddKeyId)) {
        return "Key ID must be a hexadecimal string";
    }

    return null;
}

export function encodeSetStateRequest(request: SetStateRequest): Uint8Array {
    let setStateRequest = 0x04;
    let evenKeyIdBytes = hexOctetsToUint8Array(request.evenKeyId);
    let oddKeyIdBytes = hexOctetsToUint8Array(request.oddKeyId);
    return encodeSequence([
        encodeInteger(setStateRequest),
        encodeInteger(request.stateId),
        encodeByteArray(evenKeyIdBytes),
        encodeByteArray(oddKeyIdBytes),
        encodeInteger(request.cNonce)
    ]);
}


export function decodeSetStateResponse(encodedResponse): SetStateResponse {
    let result = new SetStateResponse();
    if (!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    result.errorId = data[0] as number;
    result.responseId = data[1] as number;
    result.cryptoNonce = data[2] as number;
    return result;
}
