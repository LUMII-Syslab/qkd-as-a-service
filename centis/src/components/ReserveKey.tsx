import {useContext, useState} from "react";
import {
    decodeReserveKeyResponse,
    encodeReserveKeyRequest,
    ReserveKeyRequest,
    validateReserveKeyRequest
} from "../utils/reserve-key-req";
import ApiRequest from "./ApiRequest";
import SelectKdc from "./SelectKdc";
import InputKeyLength from "./InputKeyLength";
import InputCryptoNonce from "./InputCryptoNonce";
import {ConfigContext} from "../utils/config-context";

export default function ReserveKey() {
    const config = useContext(ConfigContext)

    let [request, setRequest] = useState({
        keyLength: 256,
        cNonce: 12345
    } as ReserveKeyRequest)

    let [kdc, setKDC] = useState("Aija")
    let endpoint = kdc === "Aija" ? config.aijaEndpoint : config.brencisEndpoint

    let error = validateReserveKeyRequest(request)
    let encoded = (request && !error) ? encodeReserveKeyRequest(request) : null

    return (
        <ApiRequest name="ReserveKeyAndGetHalf" encodedRequest={encoded} endpoint={endpoint}
                    responseDecoder={decodeReserveKeyResponse} error={error}>
            <div className="col-12 col-lg-4 my-2"><SelectKdc kdc={kdc} setKDC={setKDC}/></div>
            <div className="col-12 col-lg-4 my-2"><InputKeyLength request={request} setRequest={setRequest}/></div>
            <div className="col-12 col-lg-4 my-2"><InputCryptoNonce request={request} setRequest={setRequest}/></div>
        </ApiRequest>
    )
}