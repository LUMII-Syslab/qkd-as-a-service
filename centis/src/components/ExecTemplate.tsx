import {useEffect, useRef, useState} from "react";
import {bytesToHexOctets, bytesToSpacedHexOctets} from "../utils/formatting-bytes";
import {Collapse} from "bootstrap";
import {wsConnect, wsSendRequest} from "../utils/promise-ws";
import {errorIds, stateIds} from "../utils/translate-ids";

export default function ExecTemplate({name, encodedRequest, endpoint, responseDecoder, error, children}) {
    let [encodedResponse, setEncodedResponse] = useState(null as Uint8Array)

    return (
        <fieldset className={"p-3 my-3 shadow-sm border"}>
            <legend><code>{name}</code> request</legend>
            <RequestConfig children={children}/>
            {error && <div className="alert alert-danger my-3 alert-dismissible fade show" role="alert"> {error}</div>}
            {!error && <RequestSubmission encodedRequest={encodedRequest} endpoint={endpoint}
                                          setEncodedResponse={setEncodedResponse}/>}
            <ResponseTable encodedResponse={encodedResponse} responseDecoder={responseDecoder}/>
        </fieldset>
    )
}

function RequestConfig({children}) {
    return (
        <div className="row">
            {children}
        </div>
    )
}

function RequestSubmission({encodedRequest, endpoint, setEncodedResponse}) {
    function submit() {
        wsConnect(endpoint)
            .then(ws => wsSendRequest(ws, encodedRequest))
            .then(setEncodedResponse)
    }

    return (
        <div className="row">
            <div className="my-3 w-100 d-flex">
                <div className="flex-grow-1 me-3 border p-2">
                    ASN.1 encoded request: <code>{encodedRequest && bytesToSpacedHexOctets(encodedRequest)}</code>
                </div>
                <button className="ms-3 btn btn-outline-primary btn-sm" onClick={submit}>send request</button>
            </div>
        </div>
    )
}

function ResponseTable({encodedResponse, responseDecoder}) {
    let [collapseIcon, setCollapseIcon] = useState("bi-caret-down")
    const respTableCollapse = useRef(null)
    const collapsableRef = useRef(null)

    useEffect(() => {
        const collapsable = collapsableRef.current
        respTableCollapse.current = new Collapse(collapsable, {
            toggle: false
        })
        collapsable.addEventListener('hidden.bs.collapse', () => {
            setCollapseIcon("bi-caret-down")
        })
        collapsable.addEventListener('shown.bs.collapse', () => {
            setCollapseIcon("bi-caret-up")
        })

    }, [])

    useEffect(() => {
        if (encodedResponse) {
            respTableCollapse.current.show()
        }
    }, [encodedResponse])

    const decoded = responseDecoder(encodedResponse);

    function formatObjectKey(name: string): string {
        let res = ""
        for (let i = 0; i < name.length; i++) {
            if (name[i] === name[i].toUpperCase()) {
                res += " "
            }
            res += name[i].toLowerCase()
        }
        return res
    }

    return (
        <fieldset>
            <legend>
                <button className="btn nav-link" onClick={() => {
                    respTableCollapse.current.toggle()
                    setCollapseIcon(collapseIcon === "bi-caret-down" ? "bi-caret-up" : "bi-caret-down")
                }}>response <i className={`bi ${collapseIcon} small align-bottom`}></i></button>
            </legend>
            <div className="collapse" ref={collapsableRef}>
                <div className="mb-3 border p-2">
                    ASN.1 encoded
                    response: <code>{bytesToSpacedHexOctets(encodedResponse)}</code>
                </div>
                <table className="table table-bordered" style={{tableLayout: "fixed"}}>
                    <colgroup>
                        <col span={1} style={{width: "20%"}}/>
                        <col span={1} style={{width: "80%"}}/>
                    </colgroup>
                    <tbody>
                    {decoded && Object.keys(decoded).map((key) => {
                        if (decoded[key] instanceof Uint8Array) {
                            return (
                                <tr key={key}>
                                    <td>{formatObjectKey(key)}</td>
                                    <td><code>{bytesToHexOctets(decoded[key])}</code></td>
                                </tr>
                            )
                        }
                        else if (formatObjectKey(key)==="error id") {
                            return (
                                <tr key={key}>
                                    <td>{formatObjectKey(key)}</td>
                                    <td>{decoded[key]!==undefined && (<><code>{decoded[key]}</code> ( {errorIds[decoded[key]]} )</>)}</td>
                                </tr>
                            )
                        }
                        else if(formatObjectKey(key)==="state id") {
                            return (
                                <tr key={key}>
                                    <td>{formatObjectKey(key)}</td>
                                    <td>{decoded[key]!==undefined && (<><code>{decoded[key]}</code> ( {stateIds[decoded[key]]} )</>)}</td>
                                </tr>
                            )
                        }
                        else if (formatObjectKey(key)==="response id") {
                            return (
                                <tr key={key}>
                                    <td>{formatObjectKey(key)}</td>
                                    <td><code>{decoded[key] && `0x${decoded[key].toString(16)}`}</code></td>
                                </tr>
                            )
                        }
                        else {
                            return (
                                <tr key={key}>
                                    <td>{formatObjectKey(key)}</td>
                                    <td>{decoded[key]}</td>
                                </tr>
                            )
                        }
                    })}
                    </tbody>
                </table>
            </div>
        </fieldset>
    )
}