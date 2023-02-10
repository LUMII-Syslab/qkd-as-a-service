import {useContext, useState} from "react";
import ExecTemplate from "./ExecTemplate";
import SelectKdc from "./SelectKdc";
import InputCryptoNonce from "./InputCryptoNonce";
import {ConfigContext} from "../utils/config-context";
import {decodeGetStateResponse, encodeGetStateRequest, GetStateRequest, validateGetStateRequest} from "../utils/get-state-req";

export default function ExecGetState() {
    const config = useContext(ConfigContext)

    let [request, setRequest] = useState({
        keyLength: 256,
        keyId: "a1b2c3",
        cNonce: 12345
    } as GetStateRequest)

    let [kdc, setKDC] = useState("Aija")
    let endpoint = kdc === "Aija" ? config.aijaEndpoint : config.brencisEndpoint

    let error = validateGetStateRequest(request)
    let encoded = (request && !error) ? encodeGetStateRequest(request) : null

    return (
        <ExecTemplate name="GetState" encodedRequest={encoded} endpoint={endpoint}
                      responseDecoder={decodeGetStateResponse} error={error}>
            <div className="col-12 col-lg-2 my-2"><SelectKdc kdc={kdc} setKDC={setKDC}/></div>
            <div className="col-12 col-lg-2 my-2"><InputCryptoNonce request={request} setRequest={setRequest}/></div>
        </ExecTemplate>
    )
}