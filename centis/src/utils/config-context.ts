import {createContext} from "react";

export const ConfigContext = createContext({
    aijaEndpoint: "ws://localhost:8080/ws",
    brencisEndpoint: "ws://localhost:8081/ws",
    password: "123456789"
});