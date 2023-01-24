export default function ReserveKeyAndGetKeyHalf() {
    return (<fieldset>
            <legend><code>reserveKeyAndGetKeyHalf</code> request</legend>
            <div className="row">
                <div className="col-4">
                    <div className="form-floating">
                        <select id="rkagkh-kdc" className="form-select">
                            <option value="Aija">Aija</option>
                            <option value="Brencis">Brencis</option>
                        </select>
                        <label htmlFor="rkagkh-kdc">QKDC</label>
                    </div>
                </div>
                <div className="col-4">
                    <div className="form-floating">
                        <input type="number" id="rkagkh-key-length" defaultValue="256" className="form-control"
                               readOnly/>
                        <label htmlFor="rkagkh-key-length">Key Length</label>
                    </div>
                </div>
                <div className="col-4">
                    <div className="form-floating">
                        <input type="number" id="rkagkh-c-nonce" defaultValue="42069" className="form-control"/>
                        <label htmlFor="rkagkh-c-nonce">Crypto Nonce</label>
                    </div>
                </div>

            </div>
            <div className="responsive-input-group">
            </div>
            <div className="responsive-input-group">
            </div>
            <div className="responsive-input-group">
            </div>
            <div style={{marginTop: "1rem"}}>
                <div className="responsive-input-group" id="rkagkh-error">
                    input status: <code>?</code>
                </div>
                <div className="responsive-input-group" id="rkagkh-encoded">
                    ASN.1 encoded request: <code>?</code>
                </div>
                <div className="responsive-input-group">
                    <input id="rkagkh-send" type="button" value="SEND reserveKeyAndGetKeyHalf" className="btn"/>
                </div>
            </div>
            <fieldset>
                <legend>response</legend>

                <table className="response-table w-100">
                    <colgroup>
                        <col span="1" style={{width: "20%"}}/>
                        <col span="1" style={{width: "80%"}}/>
                    </colgroup>
                    <tr>
                        <td>crypto nonce</td>
                        <td><code id="rkagkh-resp-c-nonce">?</code></td>
                    </tr>
                    <tr>
                        <td>err code</td>
                        <td><code id="rkagkh-resp-err-code">?</code></td>
                    </tr>
                    <tr>
                        <td>key id</td>
                        <td><code id="rkagkh-resp-key-id">?</code></td>
                    </tr>
                    <tr>
                        <td>this half</td>
                        <td><code id="rkagkh-resp-this-half">?</code></td>
                    </tr>
                    <tr>
                        <td>other hash</td>
                        <td><code style={{display: "inline-flex", maxWidth: "100%", overflow: "auto"}}
                                  id="rkagkh-resp-other-hash">?</code></td>
                    </tr>
                    <tr>
                        <td>hash alg id</td>
                        <td><code id="rkagkh-resp-hash-alg-id">?</code></td>
                    </tr>
                </table>
            </fieldset>
        </fieldset>

    )
}