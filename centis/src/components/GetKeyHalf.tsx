import {useContext, useState} from "react";
import ApiRequest from "./ApiRequest";
import SelectKdc from "./SelectKdc";
import InputKeyLength from "./InputKeyLength";
import InputCryptoNonce from "./InputCryptoNonce";
import {ConfigContext} from "../utils/config-context";
import {decodeGetKeyResponse, encodeGetKeyRequest, GetKeyRequest, validateGetKeyRequest} from "../utils/get-key-req";
import InputKeyId from "./InputKeyId";

export default function GetKeyHalf() {
    const config = useContext(ConfigContext)

    let [request, setRequest] = useState({
        keyLength: 256,
        keyId: "a1b2c3",
        cNonce: 12345
    } as GetKeyRequest)

    let [kdc, setKDC] = useState("Aija")
    let endpoint = kdc === "Aija" ? config.aijaEndpoint : config.brencisEndpoint

    let error = validateGetKeyRequest(request)
    let encoded = (request && !error) ? encodeGetKeyRequest(request) : null

    return (
        <ApiRequest name="GetKeyHalf" encodedRequest={encoded} endpoint={endpoint}
                    responseDecoder={decodeGetKeyResponse} error={error}>
            <div className="col-12 col-lg-2 my-2"><SelectKdc kdc={kdc} setKDC={setKDC}/></div>
            <div className="col-12 col-lg-2 my-2"><InputKeyLength request={request} setRequest={setRequest}/></div>
            <div className="col-12 col-lg-2 my-2"><InputCryptoNonce request={request} setRequest={setRequest}/></div>
            <div className="col-12 col-lg-6 my-2"><InputKeyId request={request} setRequest={setRequest}/></div>
        </ApiRequest>
    )
}