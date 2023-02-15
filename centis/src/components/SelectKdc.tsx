export default function SelectKdc({kdc,setKDC}) {
    return (
        <div className="form-floating">
            <select className="form-select" defaultValue={kdc}
                    onChange={(event) => {
                        setKDC(event.target.value)
                    }}>
                <option value="Aija">Aija</option>
                <option value="Brencis">Brencis</option>
            </select>
            <label>KDC</label>
        </div>
    )
}