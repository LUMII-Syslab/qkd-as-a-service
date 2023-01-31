import {useEffect, useState} from "react";
import {
    wsConnect,
    wsSendRequest,
    encodeRKAGHRequest,
    parseRKAGHResponse,
    encodeGKHRequest, bytesToHexOctets, parseGKHRequest
} from "../utils/utils";

interface WatchKeysTableRow {
    KeyId: Uint8Array,
    Left: Uint8Array,
    Right: Uint8Array,
    HashLeft: Uint8Array,
    HashRight: Uint8Array,
}

export default function WatchKeys({config}) {
    let [watchingKeys, setWatchingKeys] = useState(false);
    let [requestDelay, setRequestDelay] = useState(0);
    let [tableRows, setTableRows] = useState([]); // [{keyId, left, right, hashLeft, hashRight}
    let [aijaWS, setAijaWS] = useState(null);
    let [brencisWS, setBrencisWS] = useState(null);
    
    useEffect(() => {
        wsConnect(config.aijaEndpoint).then((ws) => {
            setAijaWS(ws);
        });
        wsConnect(config.brencisEndpoint).then((ws) => {
            setBrencisWS(ws);
        });
    }, []);

    let interval = setInterval(async () => {
        if (watchingKeys) {
            let result = {} as WatchKeysTableRow

            let [rkaghReq, rkaghError] = encodeRKAGHRequest({
                config: config,
                kdc: "Aija",
                keyLength: 256,
                cNonce: 42069
            })
            if (rkaghError) {
                console.error(rkaghError)
            }
            let rkaghResp = parseRKAGHResponse(await wsSendRequest(aijaWS, rkaghReq))

            let [gkhReq, gkhError] = encodeGKHRequest({
                config: config,
                endpoint: config.brencisEndpoint,
                kdc: "Brencis",
                keyId: bytesToHexOctets(rkaghResp.keyId),
                cNonce: 42069,
                keyLength: 256,
            })
            if (gkhError) {
                console.error(gkhError)
            }
            let gkhResp = parseGKHRequest(await wsSendRequest(brencisWS, gkhReq))
            console.log(gkhResp)

            // let rkagkh_raw = await ws_send_request(aija_watch_ws, encode_rkagkh_request(256, 0))
            // let rkagkh_resp = parse_rkagkh_result(rkagkh_raw)
            // result.key_id = rkagkh_resp.key_id
            // result.key_left = rkagkh_resp.key_half
            // result.hash_right = rkagkh_resp.other_hash
            // let key_id_str = hex_octets(rkagkh_resp.key_id)
            // let gkh_raw = await ws_send_request(brencis_watch_ws, encode_gkh_request(256, key_id_str, 0))
            // let gkh_resp = parse_gkh_result(gkh_raw)
            // result.key_right = gkh_resp.key_half
            // result.hash_left = gkh_resp.other_hash
        }
    }, requestDelay)

    return (
        <>
            {/*create input to set delay for key requests*/}
            <div className={"d-flex flex-row  mb-3 col-12"}>
                <div className="form-floating col-2">
                    <input type="number" id="wk-delay" placeholder="Aija URL"
                           className="form-control"
                           defaultValue={0} onChange={
                        (e) => {
                            setRequestDelay(parseInt(e.target.value));
                        }
                    }/>
                    <label htmlFor="wk-delay">request delay in ms</label>
                </div>
                <input type="button" id="toggle-monitor" value={watchingKeys ? "STOP MONITORING" : "START MONITORING"}
                       className="btn btn-outline-dark ms-4 col-2" onClick={() => {
                    setWatchingKeys(!watchingKeys);
                }}/>
            </div>
            <table className="table table-bordered">
                <colgroup>
                    <col span={1} style={{width: "30%"}}/>
                    <col span={1} style={{width: "15%"}}/>
                    <col span={1} style={{width: "15%"}}/>
                    <col span={1} style={{width: "20%"}}/>
                    <col span={1} style={{width: "20%"}}/>
                </colgroup>
                <thead>
                <tr>
                    <th>key id</th>
                    <th>left</th>
                    <th>right</th>
                    <th>hash(left)</th>
                    <th>hash(right)</th>
                </tr>
                </thead>
                <tbody id="my-table"></tbody>
            </table>
        </>);
}