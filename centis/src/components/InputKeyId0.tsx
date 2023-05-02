export default function InputKeyId0({request, setRequest}){
    return (
        <div className="form-floating">
            <input type="text" defaultValue={request.keyId0}
                   className="form-control"
                   onChange={(event) => {
                       setRequest({...request, evenKeyId: event.target.value})
                   }}/>
            <label>Key Id 0</label>
        </div>
    )
}