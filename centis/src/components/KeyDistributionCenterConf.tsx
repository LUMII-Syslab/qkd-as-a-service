export default function KeyDistributionCenterConf({config, setConfig}: { config: any, setConfig: any }) {
    return (<fieldset className="p-3 shadow-sm border mt-3">
        <legend>KDC config</legend>
        <div className="row">
            <div className="col-4">
                <div className="form-floating">
                    <input type="url" id="kdcc-aija-url" placeholder="Aija URL" defaultValue={config.aijaEndpoint}
                           className="form-control mb-3" onChange={
                        (event) => {
                            setConfig({...config, aijaEndpoint: event.target.value})
                        }
                    }/>
                    <label htmlFor="kdcc-aija-url">Aija URL</label>
                </div>
            </div>
            <div className="col-4">
                <div className="form-floating">
                    <input type="url" id="kdcc-brencis-url" placeholder="Brencis URL"
                           defaultValue={config.brencisEndpoint} className="form-control mb-3" onChange={
                        (event) => {
                            setConfig({...config, brencisEndpoint: event.target.value})
                        }
                    }/>
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