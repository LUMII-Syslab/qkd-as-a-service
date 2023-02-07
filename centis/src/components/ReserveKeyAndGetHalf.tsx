import {useEffect, useRef, useState} from "react";
import {
    bytesToHexOctets,
    bytesToSpacedHexOctets, encodeRKAGHRequest, parseRKAGHResponse,
    RKAGHRequest, RKAGHResponse, validateRKAGHRequest,
    wsConnect,
    wsSendRequest
} from '../utils/utils';
import {Collapse} from "bootstrap";

export default function ReserveKeyAndGetHalf({config}) {
    let [request, setRequest] = useState({
        config: config, kdc: "Aija", keyLength: 256, cNonce: 42069,
    } as RKAGHRequest)
    let [endpoint, setEndpoint] = useState(config.aijaEndpoint)
    let [response, setResponse] = useState(null as RKAGHResponse)
    let [error, setError] = useState(null as string)

    function setKDC(kdc) {
        if (kdc === "Aija") {
            setEndpoint(config.aijaEndpoint)
        } else {
            setEndpoint(config.brencisEndpoint)
        }
    }

    return (<fieldset className="p-3 my-3 shadow-sm border">
        <legend><code>reserveKeyAndGetHalf</code> request</legend>
        {error && <div className="alert alert-danger alert-dismissible fade show" role="alert"> {error}</div>}
        <RKAGHReqConfig request={request} setRequest={setRequest} setKDC={setKDC}/>
        <RKAGHSubmission request={request} endpoint={endpoint} setResponse={setResponse} setParentError={setError}/>
        <RKAGHResponseTable response={response}/>
    </fieldset>)
}

let RKAGHReqConfig = ({request, setRequest, setKDC}) => {
    return (<div className="row">
        <div className="col-4">
            <div className="form-floating">
                <select className="form-select" defaultValue={request.kdc}
                        onChange={(event) => {
                            setKDC(event.target.value)
                        }}>
                    <option value="Aija">Aija</option>
                    <option value="Brencis">Brencis</option>
                </select>
                <label>KDC</label>
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
            </div>
        </div>
    </div>)

}

function RKAGHSubmission({
                             request, setResponse, setParentError, endpoint}) {
    let [error, setError] = useState(null as string)
    let [encodedRequest, setEncodedRequest] = useState(null)

    useEffect(() => {
        setError(validateRKAGHRequest(request))
        const [result, err] = encodeRKAGHRequest(request)
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
            const socket = await wsConnect(endpoint);
            let response = await wsSendRequest(socket, encodedRequest);
            socket.close();
            let result = parseRKAGHResponse(response);
            setResponse(result)

            // show response table
            let collapsable = document.getElementById('rkagh-response-table')
            if (!collapsable.classList.contains('show')) {
                new Collapse('#rkagh-response-table')
            }
        } catch (error) {
            alert("websocket connection failed: " + error.message)
        }
    }

    if (error) return null
    return (<div className="my-3 w-100 d-flex">
        <div className="flex-grow-1 me-3 border p-2">
            ASN.1 encoded request: <code>{encodedRequest && bytesToSpacedHexOctets(encodedRequest)}</code>
        </div>
        <button className="ms-3 btn btn-outline-primary btn-sm" onClick={sendRequest}>SEND REQUEST</button>
    </div>)
}

function RKAGHResponseTable({response}: { response: RKAGHResponse }) {
    let [collapseIcon, setCollapseIcon] = useState("bi-caret-down")
    const respTableCollapse = useRef(null)

    useEffect(()=>{
        respTableCollapse.current = new Collapse('#rkagh-response-table', {
            toggle: false
        })
        let collapsable = document.getElementById('rkagh-response-table')
        collapsable.addEventListener('hidden.bs.collapse', () => {
            setCollapseIcon("bi-caret-down")
        })
        collapsable.addEventListener('shown.bs.collapse', () => {
            setCollapseIcon("bi-caret-up")
        })

    },[])

    return (<fieldset>
        <legend><button className="btn nav-link" onClick={()=>{
            respTableCollapse.current.toggle()
            setCollapseIcon(collapseIcon === "bi-caret-down" ? "bi-caret-up" : "bi-caret-down")
        }}>response <i className={`bi ${collapseIcon} small align-bottom`} ></i></button></legend>

        <div className="collapse" id="rkagh-response-table">
            <div className="flex-grow-1 my-3 border p-2">
                ASN.1 encoded response: <code>{response.raw && bytesToSpacedHexOctets(response.raw)}</code>
            </div>
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
                <td>key id</td>
                <td><code>{response ? (bytesToHexOctets(response.keyId) ?? '?') : '?'}</code></td>
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
            </div>
    </fieldset>)
}
