export default function SelectKdcState({state,setKdcState}) {
    return (
        <div className="form-floating">
            <select className="form-select" defaultValue={state}
                    onChange={(event) => {
                        setKdcState(event.target.value)
                    }}>
                <option value="EMPTY">EMPTY</option>
                <option value="RECEIVING">RECEIVING</option>
                <option value="RUNNING">RUNNING</option>
            </select>
            <label>State</label>
        </div>
    )
}