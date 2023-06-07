import { useContext } from "react"
import { ConfigContext } from "../utils/config-context"
import { bytesToHexOctets } from "../utils/formatting-bytes"
import { decodeGetStateResponse, encodeGetStateRequest, GetStateRequest, GetStateResponse } from "../utils/get-state-req"
import { wsConnect, wsSendRequest } from "../utils/promise-ws"
import { encodeSetStateRequest, SetStateRequest } from "../utils/set-state-req"

export default function KdcSynchronization() {
    const config = useContext(ConfigContext)
    
    const aijaEndpoint = config.aijaEndpoint
    const brencisEndpoint = config.brencisEndpoint

    async function synchronizeKeys() {
        const aijaWs:WebSocket = await wsConnect(aijaEndpoint)        
        const brencisWs:WebSocket = await wsConnect(brencisEndpoint)
        
        const aijaGetStateRes:GetStateResponse = decodeGetStateResponse(
            await wsSendRequest(aijaWs, encodeGetStateRequest({cNonce: 12345} as GetStateRequest))
        )
        
        const brencisGetStateRes:GetStateResponse = decodeGetStateResponse(
            await wsSendRequest(brencisWs, encodeGetStateRequest({cNonce: 12345} as GetStateRequest))
        )

        await wsSendRequest(aijaWs, encodeSetStateRequest({
            stateId: 2,
            evenKeyId: bytesToHexOctets(brencisGetStateRes.oldestEvenKeyId),
            oddKeyId: bytesToHexOctets(brencisGetStateRes.oldestOddKeyId),
            cNonce: 12345
        } as SetStateRequest))

        await wsSendRequest(brencisWs, encodeSetStateRequest({
            stateId: 2,
            evenKeyId: bytesToHexOctets(aijaGetStateRes.oldestEvenKeyId),
            oddKeyId: bytesToHexOctets(aijaGetStateRes.oldestOddKeyId),
            cNonce: 12345
        } as SetStateRequest))

        aijaWs.close()
        brencisWs.close()

        alert("Keys synchronized")
    }

    return (
        <fieldset className="my-4 d-flex flex-wrap h-25 form-control shadow-sm rounded-0 p-3">
            <legend>KDC Synchronization</legend>
            <div>
                <p>
                    If one of the KDCs fails or has to be restarted, the keys have to be synchronized to reduce errors.
                </p>
                <p>
                    Synchronization performs <code>getState</code> request gathering the oldest keys of each KDC
                    followed by <code>setState</code> with to the other KDC. During <code>setState</code> if a KDC has the other KDC's oldest key then it must delete all keys that are older than it.
                </p>
            </div>
            <div className="d-flex flex-row justify-content-end w-100 my-3">
                <button className="btn btn-sm btn-outline-primary" onClick={synchronizeKeys}>Synchronize Keys</button>
            </div>
        </fieldset>
    )
}
