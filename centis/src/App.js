import diagram from './butterfly.drawio.png';
import KDCConfig from "./KDCConfig";
import ReserveKeyAndGetKeyHalf from "./ReserveKeyAndGetKeyHalf";

function App() {
    return (
        <>
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
                <ReserveKeyAndGetKeyHalf/>
            </main>
        </>
    );
}

export default App;
