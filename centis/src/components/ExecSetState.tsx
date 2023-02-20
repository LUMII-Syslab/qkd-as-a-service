import {useContext, useState} from "react";
import ExecTemplate from "./ExecTemplate";
import SelectKdc from "./SelectKdc";
import InputCryptoNonce from "./InputCryptoNonce";
import {ConfigContext} from "../utils/config-context";
import {decodeSetStateResponse, encodeSetStateRequest, SetStateRequest, validateSetStateRequest} from "../utils/set-state-req";
import SelectKdcState from "./SelectKdcState";

export default function ExecSetState() {
    const config = useContext(ConfigContext)

    let [request, setRequest] = useState({
        cNonce: 12345,
        stateId: 0,
        evenKeyId: "a1b2c3",
        oddKeyId: "a1b2c3",
    } as SetStateRequest)

    let [kdc, setKDC] = useState("Aija")
    let endpoint = kdc === "Aija" ? config.aijaEndpoint : config.brencisEndpoint

    let [state, setState] = useState("RUNNING")

    let error = validateSetStateRequest(request)
    let encoded = (request && !error) ? encodeSetStateRequest(request) : null

    return (
        <ExecTemplate name="SetState" encodedRequest={encoded} endpoint={endpoint}
                      responseDecoder={decodeSetStateResponse} error={error}>
            <div className="col-12 col-lg-2 my-2"><SelectKdc kdc={kdc} setKDC={setKDC}/></div>
            <div className="col-12 col-lg-2 my-2"><SelectKdcState state={state} setKdcState={setState}/></div>
            <div className="col-12 col-lg-2 my-2"><InputCryptoNonce request={request} setRequest={setRequest}/></div>
            <div className="col-12">
                <div className="form-control">
                    Requested state behaviour:
                    <ul>
                        <li>EMPTY - clears all keys. Once a key is received, state will change to RECEIVING</li>
                        <li>RUNNING - enables key serving</li>
                    </ul>
                    <p>
                    When state isn't RUNNING, keys are not served.
                        </p>
                    Response states:
                    <ul>
                        <li>EMPTY - there are no keys</li>
                        <li>RECEIVING - there is at least one key but keys aren't being served</li>
                        <li>RUNNING - keys are being served regardless of whether there are any</li>
                    </ul>
                    <p>
                       Note that if the state is RUNNING but there aren't any reservable keys available, the server will respond when a key becomes available instead of sending an error.
                    </p>
                    </div>
            </div>
        </ExecTemplate>
    )
}