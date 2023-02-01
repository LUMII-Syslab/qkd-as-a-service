import {StrictMode, useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';

import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import './styles/custom.scss';

import KDCConfig from "./components/KeyDistributionCenterConf";
import ReserveKeyAndGetHalf from "./components/ReserveKeyAndGetHalf";
import GetKeyHalf from "./components/GetKeyHalf";
import WatchKeys from "./components/WatchKeys";

// @ts-ignore
import diagram from './images/diagram.png';

const root = createRoot(document.getElementById('root'));

root.render(
    <StrictMode>
        <App/>
    </StrictMode>
);

function App() {
    let [config, setConfig] = useState({
        aijaEndpoint: "ws://localhost:8080/ws",
        brencisEndpoint: "ws://localhost:8081/ws",
        password: "123456789"
    })

    let [aijaConnError, setAijaConnError] = useState(null as string)
    let [brencisConnError, setBrencisConnError] = useState(null as string)
    let [testingConnections, setTestingConnections] = useState(false)

    function testConnection(endpoint: string, errorSetter: any) {
        const ws = new WebSocket(endpoint);
        ws.onopen = () => {
            errorSetter(null)
            setTestingConnections(false)
            ws.close();
        };
        ws.onerror = (e) => {
            console.log(e)
            setTestingConnections(false)
            errorSetter(e)
        }
    }

    function testConnections() {
        setTestingConnections(true)
        testConnection(config.aijaEndpoint, setAijaConnError)
        testConnection(config.brencisEndpoint, setBrencisConnError)
    }

    useEffect(testConnections, [config])

    return (
        <main className="container py-3">
            <div className="d-flex flex-wrap align-items-center">
                <div className="col-12 col-md-6">
                    <h1>Centis - QAAS admin panel</h1>
                    <p>
                        This is a simple admin panel for QAAS (QKD as a service). It allows you to send requests to
                        the KDCs (key distribution centers) and monitor the keys.
                    </p>
                </div>
                <div className="col-12 col-md-6 px-3 my-3">
                    <img src={diagram} alt="The Butterfly Protocol" className="w-100"/>
                </div>
            </div>
            <KDCConfig config={config} setConfig={setConfig}/>
            <div className="row">
                {aijaConnError && <div className="alert alert-danger alert-dismissible fade show col ms-3 me-2 my-3"
                                       role="alert">Neizdevās savienoties ar Aiju.</div>}
                {brencisConnError &&
                    <div className="alert alert-danger alert-dismissible fade show col mx-2 my-3" role="alert">
                        Neizdevās savienoties ar Brencis.</div>}
                {(aijaConnError || brencisConnError) &&
                    <button className="btn btn-warning col-3 me-3 my-3" onClick={testConnections}>
                        Pārbaudīt savienojumu <i className={`bi bi-arrow-clockwise ${testingConnections && "spinner-border"}`}></i></button>}
            </div>
            {(!aijaConnError && !brencisConnError) && !testingConnections &&
                <>
                    <h2 className={"mt-5"}>Requests</h2>
                    <ReserveKeyAndGetHalf config={config}/>
                    <GetKeyHalf config={config}/>
                    <h2 className={"mt-5"}>Monitoring</h2>
                    <WatchKeys config={config}/>
                </>
            }
        </main>
    )
}
