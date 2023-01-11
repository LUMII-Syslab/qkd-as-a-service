function hex_octets(data) {
    let res = ""
    for(let i=0;i<data.length;i++) {
        if(i) res+=" ";
        let tmp = data[i].toString(16)
        while(tmp.length<2) {
            tmp = "0"+tmp;
        }
        res += tmp;
    }
    return res
}

function ws_connect(endpoint) {
    return new Promise((resolve, reject) => {
        const socket = new WebSocket(endpoint);
        socket.onopen = () => resolve(socket);
        socket.onerror = (error) => reject(error);
    });
}

function ws_send_request(socket, request) {
    return new Promise((resolve, reject) => {
        socket.onmessage = async (event) => {
            console.log(event);
            let data = new Uint8Array(await event.data.arrayBuffer());
            let response = new Uint8Array(data);
            resolve(response);
        };
        socket.onerror = (error) => reject(error);
        socket.send(request);
    });
}