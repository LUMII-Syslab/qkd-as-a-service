import {useState} from "react";

interface RKAGKHRequest {
    kdc: string
    keyLength: number
    cNonce: number
}

interface RKAGKHResponse {
    cNonce: number
    errCode: number
    keyId: Uint8Array
    thisHalf: Uint8Array
    otherHash: Uint8Array
    hashAlgId: Uint8Array
}

export default function ReserveKeyAndGetHalf() {
    let [request, setRequest] = useState({
        kdc: "Aija", keyLength: 256, cNonce: 42069,
    } as RKAGKHRequest)

    let [response, setResponse] = useState(null as RKAGKHResponse)

    let [error, setError] = useState(null as string)

    console.log(request)

    return (<fieldset>
            <legend><code>reserveKeyAndGetKeyHalf</code> request</legend>
            {error && <div className="alert alert-danger" role="alert"> {error}</div>}
            <RKAGKHReqConfig request={request} setRequest={setRequest}/>
            <RKAGKHRSubmission request={request} setResponse={setResponse} setError={setError}/>
            <RKAGKHRResponse response={response} setResponse={setResponse}/>
        </fieldset>

    )
}

let RKAGKHReqConfig = ({request, setRequest}: { request: RKAGKHRequest, setRequest: any }) => {
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
                           }: { request: RKAGKHRequest, setResponse: any, setError: any }) {
    if (request.keyLength !== 256) {
        setError("Key length must be 256")
        return;
    }

    if (request.cNonce < 0 || request.cNonce > 65535) {
        setError("Crypto nonce must be between 0 and 65535")
        return;
    }

    const [encodedRequest, err] = encodeRKAGHRequest(request)
    if (err) {
        setError(err.message)
        return;
    }
    setError(null)

    return (<div style={{marginTop: "1rem"}}>
        <div className="responsive-input-group">
            input status: <code>?</code>
        </div>
        <div className="responsive-input-group">
            ASN.1 encoded request: <code>?</code>
        </div>
        <div className="responsive-input-group">
            <input type="button" value="SEND reserveKeyAndGetKeyHalf" className="btn"/>
        </div>
    </div>)
}

function RKAGKHRResponse({response, setResponse}) {
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
                <td><code></code></td>
            </tr>
            <tr>
                <td>err code</td>
                <td><code>?</code></td>
            </tr>
            <tr>
                <td>key id</td>
                <td><code>?</code></td>
            </tr>
            <tr>
                <td>this half</td>
                <td><code>?</code></td>
            </tr>
            <tr>
                <td>other hash</td>
                <td><code style={{display: "inline-flex", maxWidth: "100%", overflow: "auto"}}
                >?</code></td>
            </tr>
            <tr>
                <td>hash alg id</td>
                <td><code>?</code></td>
            </tr>
            </tbody>
        </table>
    </fieldset>)
}

function encodeRKAGHRequest(request: RKAGKHRequest): [Uint8Array, Error] {
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
