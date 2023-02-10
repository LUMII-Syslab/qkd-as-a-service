import {useContext, useState} from "react";
import ExecTemplate from "./ExecTemplate";
import SelectKdc from "./SelectKdc";
import InputCryptoNonce from "./InputCryptoNonce";
import {ConfigContext} from "../utils/config-context";
import {decodeSetStateResponse, encodeSetStateRequest, SetStateRequest, validateSetStateRequest} from "../utils/set-state-req";

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

    let error = validateSetStateRequest(request)
    let encoded = (request && !error) ? encodeSetStateRequest(request) : null

    return (
        <ExecTemplate name="SetState" encodedRequest={encoded} endpoint={endpoint}
                      responseDecoder={decodeSetStateResponse} error={error}>
            <div className="col-12 col-lg-2 my-2"><SelectKdc kdc={kdc} setKDC={setKDC}/></div>
            <div className="col-12 col-lg-2 my-2"><InputCryptoNonce request={request} setRequest={setRequest}/></div>
        </ExecTemplate>
    )
}