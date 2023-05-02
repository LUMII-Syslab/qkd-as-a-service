import {createContext} from "react";

export const ConfigContext = createContext({
    aijaEndpoint: "ws://localhost:8001/ws",
    brencisEndpoint: "ws://localhost:8002/ws",
    password: "123456789"
});