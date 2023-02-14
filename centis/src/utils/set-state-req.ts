import {ASNDERToList} from "./formatting-bytes";

export class SetStateRequest {
    cNonce: number
    stateId: number
    evenKeyId: string
    oddKeyId: string

}

export class SetStateResponse {
    errCode: number
    responseId: number
    cryptoNonce: number
}

export function validateSetStateRequest(request: SetStateRequest): string {
    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    if (request.stateId !== 0 && request.stateId !== 1 && request.stateId !== 2) {
        return "State ID must be 0, 1 or 2";
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
    const eLen = request.evenKeyId.length;
    const oLen = request.oddKeyId.length;
    const result = new Uint8Array(14+eLen+oLen)
    // sequence
    result[0] = 0x30;
    result[1] = result.length-2
    // setState request
    result[2] = 0x02;
    result[3] = 0x01
    result[4] = 0x04
    // crypto nonce
    result[5] = 0x02;
    result[6] = 0x02
    result[7] = request.cNonce >> 8;
    result[8] = request.cNonce % 256
    // state ID
    result[9] = 0x02;
    result[10] = 0x01
    result[11] = request.stateId
    // even key ID
    result[12] = 0x04;
    result[13] = eLen
    for (let i = 0; i < eLen; i++) {
        result[14 + i] = parseInt(request.evenKeyId.substr(i * 2, 2), 16);
    }
    // odd key ID
    result[13+eLen] = 0x04;
    result[13+eLen+1] = oLen
    for (let i = 0; i < oLen; i++) {
        result[13 + eLen + 2 + i] = parseInt(request.oddKeyId.substr(i * 2, 2), 16);
    }
    return result;
}


export function decodeSetStateResponse(encodedResponse): SetStateResponse {
    let result = new SetStateResponse();
    if (!encodedResponse) return result;
    let data = ASNDERToList(encodedResponse);
    result.errCode = data[0] as number;
    result.responseId = data[1] as number;
    result.cryptoNonce = data[2] as number;
    return result;
}
