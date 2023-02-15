export function wsConnect(endpoint): Promise<WebSocket> {
    return new Promise((resolve, reject) => {
        const socket = new WebSocket(endpoint);
        socket.onopen = () => resolve(socket);
        socket.onerror = (error) => reject(error);
    });
}

export function wsSendRequest(socket, request) {
    return new Promise((resolve, reject) => {
        socket.onmessage = async (event) => {
            let data = new Uint8Array(await event.data.arrayBuffer());
            let response = new Uint8Array(data);
            resolve(response);
        };
        socket.onerror = (error) => reject(error);
        socket.send(request);
    });
}