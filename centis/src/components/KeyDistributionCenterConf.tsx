export default function KeyDistributionCenterConf({config, setConfig}: { config: any, setConfig: any }) {
    let handleAijaEndpointChange = (event: any) =>
        setConfig({...config, aijaEndpoint: event.target.value})

    let handleBrencisEndpointChange = (event: any) =>
        setConfig({...config, brencisEndpoint: event.target.value})

    let handlePasswordChange = (event: any) =>
        setConfig({...config, password: event.target.value})

    return (
        <fieldset className="p-3 shadow-sm border mt-3">
            <legend>KDC config</legend>
            <div className="row">
                <div className="col-12 col-md-4">
                    <div className="form-floating">
                        <input type="url" placeholder="Aija URL" defaultValue={config.aijaEndpoint}
                               className="form-control mb-3" onChange={handleAijaEndpointChange}/>
                        <label>Aija URL</label>
                    </div>
                </div>
                <div className="col-12 col-md-4">
                    <div className="form-floating">
                        <input type="url" placeholder="Brencis URL" defaultValue={config.brencisEndpoint}
                               className="form-control mb-3" onChange={handleBrencisEndpointChange}/>
                        <label>Brencis URL</label>
                    </div>
                </div>
                <div className="col-12 col-md-4">
                    <div className="form-floating">
                        <input type="password" placeholder="Password" defaultValue="1234568910"
                               className="form-control mb-3" onChange={handlePasswordChange}/>
                        <label>Password</label>
                    </div>
                </div>
            </div>
        </fieldset>);
}