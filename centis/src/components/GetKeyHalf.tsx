import {useState, useEffect} from "react";
import {
    bytesToSpacedHexOctets,
    wsConnect,
    wsSendRequest,
    bytesToHexOctets, parseGKHRequest, GKHResponse, GKHRequest, validateGKHRequest, encodeGKHRequest
} from '../utils/utils';


export default function ReserveKeyAndGetHalf({config}) {
    let [request, setRequest] = useState({
        config: config,
        kdc: "Aija",
        keyLength: 256,
        cNonce: 42069,
        keyId: "e47908469a325a00eb566e5602e213f2e5429666acd96a47cf31871c98eafd8c"
    } as GKHRequest)

    let [response, setResponse] = useState(null as GKHResponse)

    let [error, setError] = useState(null as string)

    return (<fieldset>
            <legend><code>getKeyHalf</code> request</legend>
            {error && <div className="alert alert-danger alert-dismissible fade show" role="alert"> {error}</div>}
            <GKHReqConfig request={request} setRequest={setRequest}/>
            <GKHSubmission request={request} setResponse={setResponse} setParentError={setError}/>
            <GKHResponseTable response={response}/>
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
        setError(validateGKHRequest(request))
        const [result, err] = encodeGKHRequest(request)
        if (err) {
            setError(err.message)
            return
        }
        setEncodedRequest(result)
    }, [request])

    useEffect(() => {
        setParentError(error)
    }, [error, setParentError])

    async function sendRequest() {
        try {
            let endpoint = request.config.aijaEndpoint;
            if (request.kdc === "Brencis") endpoint = request.config.brencisEndpoint
            console.log("connecting to " + endpoint)
            const socket = await wsConnect(endpoint);
            let response = await wsSendRequest(socket, encodedRequest);
            socket.close();
            let parsed = parseGKHRequest(response);
            setResponse(parsed)
        } catch (error) {
            alert("websocket connection failed: " + error.message)
        }

    }

    if (error) return null
    return (<div className="my-3 w-100 d-flex">
        <div className="flex-grow-1 me-3 border p-2">
            ASN.1 encoded request: <code>{encodedRequest && bytesToSpacedHexOctets(encodedRequest)}</code>
        </div>
        <button className="ms-3 btn btn-outline-dark" onClick={sendRequest}>SEND REQUEST</button>
    </div>)
}

function GKHResponseTable({response}: { response: GKHResponse }) {
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
