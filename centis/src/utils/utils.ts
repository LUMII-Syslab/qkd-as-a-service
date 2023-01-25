export function bytesToHexOctets(data) {
    let res = ""
    for (let i = 0; i < data.length; i++) {
        let tmp = data[i].toString(16)
        while (tmp.length < 2) {
            tmp = "0" + tmp;
        }
        res += tmp;
    }
    return res
}

export function bytesToSpacedHexOctets(data) {
    let res2 = ""
    for (let i = 0; i < data.length; i++) {
        if (i) res2 += " ";
        let tmp = data[i].toString(16)
        while (tmp.length < 2) {
            tmp = "0" + tmp;
        }
        res2 += tmp;
    }
    return res2
}

export function wsConnect(endpoint) {
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

export function ASNDERToList(seq): (number | Uint8Array)[] {
    let it = 2;
    let data = [];
    while (it < 2 + seq[1]) {
        if (seq[it] == 0x02) { // an integer follows
            it++;
            let int_len = seq[it++];
            let int = 0;
            for (let i = 0; i < int_len; i++) {
                int <<= 8;
                int += seq[it++];
            }
            data.push(int);
        } else if (seq[it] == 0x04 || seq[it] == 0x06) {
            it++;
            let arr_len = seq[it++];
            let res = new Uint8Array(arr_len);
            for (let i = 0; i < arr_len; i++) {
                res[i] = seq[it++];
            }
            data.push(res);
        } else {
            console.error("ERROR converting DER to list", seq[it]);
            return;
        }
    }
    return data;
}

export function hexOctetsToUint8Array(hexOctetStr: string): Uint8Array {
    let res = new Uint8Array(hexOctetStr.length / 2);
    for (let i = 0; i < res.length; i++) {
        res[i] = parseInt(hexOctetStr.substr(i * 2, 2), 16);
    }
    return res;
}