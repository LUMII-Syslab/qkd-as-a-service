export default function InputCryptoNonce({request, setRequest}) {
    return (
        <div className="form-floating">
            <input type="number" defaultValue={request.cNonce} className="form-control"
                   onChange={(event) => {
                       setRequest({...request, cNonce: event.target.value})
                   }}/>
            <label>Crypto Nonce</label>
        </div>
    )
}