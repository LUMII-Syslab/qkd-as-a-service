export default function KDCConfig() {
    return (<fieldset>
        <legend>KDC config</legend>
        <div className="row">
            <div className="col-4">
                <div className="form-floating">
                    <input type="url" id="kdcc-aija-url" placeholder="Aija URL" defaultValue="ws://localhost:8080/ws"
                           className="form-control mb-3"
                    />
                    <label htmlFor="kdcc-aija-url">Aija URL</label>
                </div>
            </div>
            <div className="col-4">
                <div className="form-floating">
                    <input type="url" id="kdcc-brencis-url" placeholder="Brencis URL"
                           defaultValue="ws://localhost:8081/ws" className="form-control mb-3"/>
                    <label htmlFor="kdcc-brencis-url">Brencis URL</label>
                </div>
            </div>
            <div className="col-4">
                <div className="form-floating">
                    <input type="password" id="kdcc-password" placeholder="Password" className="form-control mb-3"
                           defaultValue="1234568910"/>
                    <label htmlFor="kdcc-password">Password</label>
                </div>
            </div>
        </div>
    </fieldset>);
}