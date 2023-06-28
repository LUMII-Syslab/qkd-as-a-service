export default function InputKeyLength({request, setRequest}) {
    return (
        <div className="form-floating">
            <input type="number" defaultValue={request.keyLength}
                   className="form-control"
                   readOnly disabled/>
            <label>Key Length [bits]</label>
        </div>
    )
}