export default function InputKeyId({request, setRequest}){
    return (
        <div className="form-floating">
            <input type="text" defaultValue={request.keyId}
                   className="form-control"
                   onChange={(event) => {
                       setRequest({...request, keyId: event.target.value})
                   }}/>
            <label>Key Id</label>
        </div>
    )
}