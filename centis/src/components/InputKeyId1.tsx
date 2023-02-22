export default function InputKeyId1({request, setRequest}){
    return (
        <div className="form-floating">
            <input type="text" defaultValue={request.keyId1}
                   className="form-control"
                   onChange={(event) => {
                       setRequest({...request, KeyId1: event.target.value})
                   }}/>
            <label>Key Id 1</label>
        </div>
    )
}