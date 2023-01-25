import {useState, useEffect} from "react";
// @ts-ignore
import {bytesToSpacedHexOctets, wsConnect, wsSendRequest, ASNDERToList} from '../utils/utils.ts';

interface RKAGHRequest {
    config: any
    kdc: string
    keyLength: number
    cNonce: number
}

interface RKAGHResponse {
    cNonce: number
    errCode: number
    keyId: Uint8Array
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
}

export default function ReserveKeyAndGetHalf({config}) {
    let [request, setRequest] = useState({
        config: config, kdc: "Aija", keyLength: 256, cNonce: 42069,
    } as RKAGHRequest)

    let [response, setResponse] = useState(null as RKAGHResponse)

    let [error, setError] = useState(null as string)

    useEffect(() => {
        console.log("hello")
    })

    return (<fieldset>
            <legend><code>reserveKeyAndGetHalf</code> request</legend>
            {error && <div className="alert alert-danger" role="alert"> {error}</div>}
            <RKAGKHReqConfig request={request} setRequest={setRequest}/>
            <RKAGKHRSubmission request={request} setResponse={setResponse} setError={setError}/>
            <RKAGKHRResponse response={response}/>
        </fieldset>
    )
}

let RKAGKHReqConfig = ({request, setRequest}: { request: RKAGHRequest, setRequest: any }) => {
    return (<div className="row">
        <div className="col-4">
            <div className="form-floating">
                <select className="form-select" defaultValue={request.kdc}
                        onChange={(event) => {
                            setRequest({...request, kdc: event.target.value})
                        }}>
                    <option value="Aija">Aija</option>
                    <option value="Brencis">Brencis</option>
                </select>
                <label>QKDC</label>
            </div>
        </div>
        <div className="col-4">
            <div className="form-floating">
                <input type="number" defaultValue={request.keyLength}
                       className="form-control"
                       readOnly disabled/>
                <label>Key Length</label>
            </div>
        </div>
        <div className="col-4">
            <div className="form-floating">
                <input type="number" defaultValue={request.cNonce} className="form-control"
                       onChange={(event) => {
                           setRequest({...request, cNonce: event.target.value})
                       }}/>
                <label>Crypto Nonce</label>
                <div className="invalid-feedback">
                    Please choose a username.
                </div>
            </div>
        </div>
    </div>)

}

function RKAGKHRSubmission({
                               request, setResponse, setError
                           }: { request: RKAGHRequest, setResponse: any, setError: any }) {

    if (request.keyLength !== 256) {
        console.error("Key length must be 256")
        setError("Key length must be 256")
        return;
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        console.error("Crypto nonce must be between 0 and 65535")
        setError("Crypto nonce must be between 0 and 65535")
        return;
    }

    const [encodedRequest, err] = encodeRKAGHRequest(request)
    if (err) {
        console.error(err)
        setError(err.message)
        return;
    }

    async function sendRequest() {
        try {
            let endpoint;
            if (request.kdc === "Aija") endpoint = request.config.aijaEndpoint
            else if (request.kdc === "Brencis") endpoint = request.config.brencisEndpoint
            else {
                setError("unknown kdc: " + request.kdc)
            }
            let socket = await wsConnect(endpoint);
            let response = await wsSendRequest(socket, encodedRequest);
            let parsed = parseRKAGHRequest(response);
            console.log(parsed);
        } catch (error) {
            console.error("websocket connection failed " + error.message)
            setError("websocket connection failed " + error.message)
        }

    }

    return (<div className="my-3 w-100 d-flex">
        <div className="flex-grow-1 me-3 border p-2">
            ASN.1 encoded request: <code>{bytesToSpacedHexOctets(encodedRequest)}</code>
        </div>
        <button className="ms-3 btn btn-primary" onClick={sendRequest}>SEND REQUEST</button>
    </div>)
}

function RKAGKHRResponse({response}: { response: RKAGHResponse }) {
    return (<fieldset>
        <legend>response</legend>

        <table className="table table-bordered w-100">
            <colgroup>
                <col span={1} style={{width: "20%"}}/>
                <col span={1} style={{width: "80%"}}/>
            </colgroup>
            <tbody>
            <tr>
                <td>crypto nonce</td>
                <td><code>{response ? (response.cNonce ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>err code</td>
                <td><code>{response ? (response.errCode ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>key id</td>
                <td><code>{response ? (response.keyId ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>this half</td>
                <td><code>{response ? (response.thisHalf ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>other hash</td>
                <td><code style={{display: "inline-flex", maxWidth: "100%", overflow: "auto"}}
                >{response ? (response.otherHash ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>hash alg id</td>
                <td><code>{response ? (response.hashAlgId ?? '?') : '?'}</code></td>
            </tr>
            </tbody>
        </table>
    </fieldset>)
}

function encodeRKAGHRequest(request: RKAGHRequest): [Uint8Array, Error] {
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

function parseRKAGHRequest(msg_arr): RKAGHResponse {
    let data = ASNDERToList(msg_arr);
    let result = {} as RKAGHResponse;
    result.cNonce = data[1] as number;
    result.errCode = data[2] as number;
    result.keyId = data[3] as Uint8Array;
    result.thisHalf = data[4] as Uint8Array;
    result.otherHash = data[5] as Uint8Array;
    result.hashAlgId = data[6] as Uint8Array;
    return result;
}