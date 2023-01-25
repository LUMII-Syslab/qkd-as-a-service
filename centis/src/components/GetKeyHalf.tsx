import {useState, useEffect} from "react";
// @ts-ignore
import {
    bytesToSpacedHexOctets,
    wsConnect,
    wsSendRequest,
    ASNDERToList,
    hexOctetsToUint8Array,
    bytesToHexOctets
} from '../utils/utils.ts';

interface GKHRequest {
    config: any
    endpoint: string
    kdc: string
    keyLength: number
    cNonce: number
    keyId: string
}

interface GKHResponse {
    cNonce: number
    errCode: number
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
}

export default function ReserveKeyAndGetHalf({config}) {
    let [request, setRequest] = useState({
        config: config, kdc: "Aija", keyLength: 256, cNonce: 42069,
    } as GKHRequest)

    let [response, setResponse] = useState(null as GKHResponse)

    let [error, setError] = useState(null as string)

    return (<fieldset>
            <legend><code>getKeyHalf</code> request</legend>
            {error && <div className="alert alert-danger alert-dismissible fade show" role="alert"> {error}</div>}
            <GKHReqConfig request={request} setRequest={setRequest}/>
            <GKHSubmission request={request} setResponse={setResponse} setParentError={setError}/>
            <GKHResponse response={response}/>
        </fieldset>
    )
}

let GKHReqConfig = ({request, setRequest}) => {
    return (<div className="row">
        <div className="col-2">
            <div className="form-floating">
                <select className="form-select" defaultValue={request.kdc}
                        onChange={(event) => {
                            setRequest({...request, kdc: event.target.value})
                        }}>
                    <option value="Aija">Aija</option>
                    <option value="Brencis">Brencis</option>
                </select>
                <label>KDC</label>
            </div>
        </div>
        <div className="col-2">
            <div className="form-floating">
                <input type="number" defaultValue={request.keyLength}
                       className="form-control"
                       readOnly disabled/>
                <label>Key Length</label>
            </div>
        </div>
        <div className="col-2">
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
        <div className="col-6">
            <div className="form-floating">
                <input type="text" defaultValue={request.keyId}
                       className="form-control"
                       onChange={(event) => {
                           setRequest({...request, keyId: event.target.value})
                       }}/>
                <label>Key Id</label>
            </div>
        </div>
    </div>)

}

function GKHSubmission({
                           request, setResponse, setParentError
                       }: { request: GKHRequest, setResponse: any, setParentError: any }) {
    let [error, setError] = useState(null as string)
    let [encodedRequest, setEncodedRequest] = useState(null)

    useEffect(() => {
        setError(null)
        error = validateGKHRequest(request)
        setError(error)
        console.log(error, request)

        const [result, err] = encodeGKHRequest(request)
        if (err) {
            setError(err.message)
            return
        }

        setEncodedRequest(result)
    }, [request])

    useEffect(() => {
        setParentError(error)
    }, [error])

    async function sendRequest() {
        try {
            let endpoint = request.config.aijaEndpoint;
            if (request.kdc === "Brencis") endpoint = request.config.brencisEndpoint
            console.log("connecting to " + endpoint)
            let socket = await wsConnect(endpoint);
            let response = await wsSendRequest(socket, encodedRequest);
            let parsed = parseGKHRequest(response);
            setResponse(parsed)
        } catch (error) {
            alert("websocket connection failed: " + error.message)
        }

    }

    if (error) return null
    return (<div className="my-3 w-100 d-flex">
        <div className="flex-grow-1 me-3 border p-2">
            ASN.1 encoded request: <code>{encodedRequest}</code>
        </div>
        <button className="ms-3 btn btn-primary" onClick={sendRequest}>SEND REQUEST</button>
    </div>)
}

function GKHResponse({response}: { response: GKHResponse }) {
    return (<fieldset>
        <legend>response</legend>

        <table className="table table-bordered w-100" style={{tableLayout: "fixed"}}>
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
                <td>this half</td>
                <td><code>{response ? (bytesToHexOctets(response.thisHalf) ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>other hash</td>
                <td><code style={{display: "inline-flex", maxWidth: "100%", overflow: "auto"}}
                >{response ? (bytesToHexOctets(response.otherHash) ?? '?') : '?'}</code></td>
            </tr>
            <tr>
                <td>hash alg id</td>
                <td><code>{response ? (bytesToHexOctets(response.hashAlgId) ?? '?') : '?'}</code></td>
            </tr>
            </tbody>
        </table>
    </fieldset>)
}

function encodeGKHRequest(request: GKHRequest): [Uint8Array, Error] {
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

function parseGKHRequest(msg_arr): GKHResponse {
    let data = ASNDERToList(msg_arr);
    let result = {} as GKHResponse;
    result.cNonce = data[1] as number;
    result.errCode = data[2] as number;
    result.thisHalf = data[3] as Uint8Array;
    result.otherHash = data[4] as Uint8Array;
    result.hashAlgId = data[5] as Uint8Array;
    return result;
}

function validateGKHRequest(request: GKHRequest) {
    if (request.keyLength !== 256) {
        return "Key length must be 256"
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        return "Crypto nonce must be between 0 and 65535"
    }

    if (request.kdc !== "Aija" && request.kdc !== "Brencis") {
        return "Unknown KDC"
    }

    if (!/^[0-9A-F]+$/i.test(request.keyId)) {
        return "Key ID must be a hexadecimal number";
    }

    return null;
}