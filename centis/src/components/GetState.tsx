import {
    bytesToSpacedHexOctets, encodeGetStateRequest,
    GetStateRequest,
    GetStateResponse, parseGetStateResponse,
    validateGetStateRequest,
    wsConnect,
    wsSendRequest
} from "../utils/utils";
import {useEffect, useRef, useState} from "react";
import {Collapse} from "bootstrap";

export default function GetState({config}) {
    let [request, setRequest] = useState({
        cNonce: 42069,
    } as GetStateRequest)

    let [response, setResponse] = useState(null as GetStateResponse)
    let [endpoint, setEndpoint] = useState(config.aijaEndpoint)
    let [error, setError] = useState(null as string)

    function setKDC(kdc) {
        if (kdc === "Aija") {
            setEndpoint(config.aijaEndpoint)
        } else {
            setEndpoint(config.brencisEndpoint)
        }
    }

    return (
        <fieldset className={"p-3 my-3 shadow-sm border"}>
            <legend><code>getState</code> request</legend>
            {error && <div className="alert alert-danger alert-dismissible fade show" role="alert"> {error}</div>}
            <GetStateConfig request={request} setRequest={setRequest} setKDC={setKDC}/>
            <GetStateSubmission request={request} setResponse={setResponse} setParentError={setError}
                                endpoint={config.aijaEndpoint}/>
            <GetStateResponseTable response={response}/>
        </fieldset>
    )
}

function GetStateConfig({request, setRequest, setKDC}){
    return (<div className="row">
        <div className="col-2">
            <div className="form-floating">
                <select className="form-select" defaultValue={"Aija"}
                        onChange={(event) => {
                            setKDC(event.target.value);
                        }}>
                    <option value="Aija">Aija</option>
                    <option value="Brencis">Brencis</option>
                </select>
                <label>KDC</label>
            </div>
        </div>
        <div className="col-2">
            <div className="form-floating">
                <input type="number" defaultValue={42069} className="form-control" onChange={(event)=> {
                    setRequest({...request, cNonce: event.target.value})
                }}/>
                <label>Crypto Nonce</label>
            </div>
        </div>
    </div>)
}

function GetStateSubmission({
                                request, setResponse, setParentError, endpoint
                            }) {
    let [error, setError] = useState(null as string)
    let [encodedRequest, setEncodedRequest] = useState(null)

    useEffect(() => {
        setError(validateGetStateRequest(request))
        const [result, err] = encodeGetStateRequest(request)
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
            let parsed = parseGetStateResponse(response);
            console.log(parsed)
            setResponse(parsed)

            // show response table
            let collapsable = document.getElementById('getstate-response-table')
            if (!collapsable.classList.contains('show')) {
                new Collapse('#getstate-response-table')
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

function GetStateResponseTable({response}: { response: GetStateResponse }) {
    let [collapseIcon, setCollapseIcon] = useState("bi-caret-down")
    const respTableCollapse = useRef(null)

    useEffect(() => {
        respTableCollapse.current = new Collapse('#getstate-response-table', {
            toggle: false
        })
        let collapsable = document.getElementById('getstate-response-table')
        collapsable.addEventListener('hidden.bs.collapse', () => {
            setCollapseIcon("bi-caret-down")
        })
        collapsable.addEventListener('shown.bs.collapse', () => {
            setCollapseIcon("bi-caret-up")
        })

    }, [])

    return (<fieldset>
        <legend>
            <button className="btn nav-link" onClick={() => {
                respTableCollapse.current.toggle()
                setCollapseIcon(collapseIcon === "bi-caret-down" ? "bi-caret-up" : "bi-caret-down")
            }}>response <i className={`bi ${collapseIcon} small align-bottom`}></i></button>
        </legend>

        <div className="collapse" id="getstate-response-table">
            <table className="table table-bordered">
                <colgroup>
                    <col span={1} style={{width: "20%"}}/>
                    <col span={1} style={{width: "80%"}}/>
                </colgroup>
                <tbody>
                <tr>
                    <td>state</td>
                    <td><code>{response ? (response.state ?? '?'):'?'}</code></td>
                </tr>
                <tr>
                    <td>keyID<sub>0</sub></td>
                    <td><code>{response ? (response.keyId0 ?? '?'):'?'}</code></td>
                </tr>
                <tr>
                    <td>keyID<sub>1</sub></td>
                    <td><code>{response ? (response.keyId1 ?? '?'):'?'}</code></td>
                </tr>
                </tbody>
            </table>
        </div>
    </fieldset>)
}