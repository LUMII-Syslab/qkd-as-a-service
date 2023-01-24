import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import 'bootstrap/dist/js/bootstrap.bundle.min';
import './styles/lux.css'
import './styles/cosmo.css'
import './styles/global.css'

const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
    <React.StrictMode>
        <App/>
    </React.StrictMode>
);
