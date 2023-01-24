import {useState} from "react";

interface RKAGKHRequest {
    kdc: string
    keyLength: number
    cNonce: number
}

export default function ReserveKeyAndGetKeyHalf() {
    let [request, setRequest] = useState({
        kdc: "Aija",
        keyLength: 256,
        cNonce: 42069,
    })

    let [response, setResponse] = useState({
        cNonce: '?1',
        errCode: '?2',
        keyId: '?3',
        thisHalf: '?4',
        otherHash: '?5',
        hashAlgId: '?6'
    })

    console.log(request)

    return (<fieldset>
            <legend><code>reserveKeyAndGetKeyHalf</code> request</legend>
            <RKAGKHReqConfig request={request} setRequest={setRequest}/>
            <RKAGKHRSubmission/>
            <RKAGKHRResponse response={response} setResponse={setResponse}/>
        </fieldset>

    )
}

let RKAGKHReqConfig = ({request, setRequest}) => {
    return (
        <div className="row">
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
                </div>
            </div>
        </div>
    )

}

let RKAGKHRSubmission = () => {
    return (
        <div style={{marginTop: "1rem"}}>
            <div className="responsive-input-group">
                input status: <code>?</code>
            </div>
            <div className="responsive-input-group">
                ASN.1 encoded request: <code>?</code>
            </div>
            <div className="responsive-input-group">
                <input type="button" value="SEND reserveKeyAndGetKeyHalf" className="btn"/>
            </div>
        </div>
    )
}

let RKAGKHRResponse = ({response, setResponse}) => {
    return (
        <fieldset>
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
        </fieldset>
    )
}