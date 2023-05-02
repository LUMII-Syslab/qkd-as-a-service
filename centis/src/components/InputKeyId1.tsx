import { SetStateRequest } from "../utils/set-state-req"

export default function InputKeyId1({request, setRequest}){
    return (
        <div className="form-floating">
            <input type="text" defaultValue={request.keyId1}
                   className="form-control"
                   onChange={(event) => {
                       setRequest({...request, oddKeyId: event.target.value} as SetStateRequest)
                   }}/>
            <label>Key Id 1</label>
        </div>
    )
}