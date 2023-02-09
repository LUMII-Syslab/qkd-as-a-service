import {ASNDERToList} from "./utils";

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