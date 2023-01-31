import {StrictMode, useState} from 'react';
import {createRoot} from 'react-dom/client';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import './styles/custom.scss';
import KDCConfig from "./components/KeyDistributionCenterConf";
import ReserveKeyAndGetHalf from "./components/ReserveKeyAndGetHalf";
import GetKeyHalf from "./components/GetKeyHalf";
// @ts-ignore
import diagram from './images/diagram.png';
import WatchKeys from "./components/WatchKeys";

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

    return (
        <main className="container py-3">
            <div className="d-flex">
                <div className="col-md-6">
                    <h1>Centis - QAAS admin panel</h1>
                    <p>
                        This is a simple admin panel for QAAS (QKD as a service). It allows you to send requests to
                        the KDCs (key distribution centers) and monitor the keys.
                    </p>
                </div>
                <div className="col-md-6">
                    <img src={diagram} alt="The Butterfly Protocol" className="w-100"/>
                </div>
            </div>
            <KDCConfig config={config} setConfig={setConfig}/>
            <h2 className={"mt-5"}>Requests</h2>
            <ReserveKeyAndGetHalf config={config}/>
            <GetKeyHalf config={config}/>
            <h2 className={"mt-5"}>Monitoring</h2>
            <WatchKeys config={config}/>
        </main>
    )
}
