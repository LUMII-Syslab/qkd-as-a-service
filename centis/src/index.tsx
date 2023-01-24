import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import 'bootstrap/dist/js/bootstrap.bundle.min';
import './styles/lux.css'
import './styles/cosmo.css'
import './styles/global.css'
import KDCConfig from "./components/KeyDistributionCenterConf";
// @ts-ignore
import ReserveKeyAndGetHalf from "./components/ReserveKeyAndGetHalf.tsx";
// @ts-ignore
import diagram from './images/diagram.png';

const root = createRoot(document.getElementById('root'));

root.render(
    <StrictMode>
        <main className="container py-3">
            <div style={{display: "inline-flex"}}>
                <div style={{width: "50%"}}>
                    <h1>Centis - QAAS admin panel</h1>
                    <p>
                        This is a simple admin panel for QAAS (QKD as a service). It allows you to send requests to
                        the KDCs
                        (key
                        distribution centers) and monitor the
                        keys.
                    </p>
                </div>
                <div style={{width: "50%"}}>
                    <img src={diagram} alt="The Butterfly Protocol" style={{padding: "30px"}}/>
                </div>
            </div>
            <KDCConfig/>
            <h2>Requests</h2>
            <ReserveKeyAndGetHalf/>
        </main>
    </StrictMode>
);
