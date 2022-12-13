let watch_keys = false
let watching_keys = false

function start_watching_keys() {
    if (watching_keys) return
    watching_keys = true
    watch_keys = true
    let socket = new WebSocket("ws://localhost:8080/ws")
    console.log("attempting WebSocket Connection")

    socket.onopen = () => {
        console.log("successfully connected")
        function add_key() {
            socket.onmessage = (msg) => {
                let key_id = msg.data
                console.log("key_id: ", key_id)
                socket.onmessage = (msg) => {
                    let key_left = msg.data
                    let table = document.getElementById("my-table")
                    let row = table.insertRow(0)
                    row.innerHTML = `<td>${key_id}</td><td>${key_left}</td>`
                    if (table.rows.length > 10)
                        table.rows[10].remove()
                    if(watch_keys)
                        add_key()
                }
                socket.send(`get_left ${key_id}`)
            }
            socket.send("reserve")
        }
        add_key()
    }

    socket.onclose = (event) =>  console.log("socket closed connection: ", event)
    socket.onerror = (error) => console.log("socket error: ",error)
}

function stop_watching_keys() {
    watch_keys = false
    watching_keys = false
}

function reserve_key_and_get_half(socket) {
    const req = new Uint8Array(13)
    // sequence
    req[0] = 0x30; req[1] = 0x0B    // a sequence of 11 bytes will follow

    // reserveKeyAndGetKeyHalf request
    req[2] = 0x02; req[3] = 0x01    // an integer of 1 byte will follow
    req[4] = 0x01
    
    // requested key length (256)
    req[5] = 0x02; req[6] = 0x02    // an integer of 2 bytes will follow
    req[7] = 0x01; req[8] = 0x00

    // call#
    req[9] = 0x02; req[10] = 0x02   // an integer of 2 bytes will follow
    req[11] = 0x30; req[12] = 0x39
    socket.send(req)
}

function get_key_half(socket, key_id) {
    const req = new Uint8Array(15+key_id.length)
    // sequence
    req[0] = 0x30; req[1] = 13+key_id.length    // a sequence of 13+len(key_id) bytes will follow

    // getKeyHalf request
    req[2] = 0x02; req[3] = 0x01    // an integer of 1 byte will follow
    req[4] = 0x02

    // key length
    req[5] = 0x02; req[6] = 0x02    // an integer of 2 bytes will follow
    req[7] = 0x01; req[8] = 0x00    // requested key length (256)

    // key id
    req[9] = 0x04; req[10] = key_id.length  // a byte array of key_id.length bytes will follow
    for(let i=0;i<key_id.length;i++)
        req[i+11] = key_id[i];
    
    // call#
    req[11+key_id.length] = 0x02; req[12+key_id.length] = 0x02   // an integer of 2 bytes will follow
    req[13+key_id.length] = 0x30; req[14+key_id.length] = 0x39

    socket.send(req)
}

function ASN_DER_to_list(seq) {
    let it = 2;
    let data = [];
    while(it<2+seq[1]) {
        if(seq[it]==0x02) { // an integer follows
            it++;
            let int_len = seq[it++];
            let int = 0;
            for(let i=0;i<int_len;i++){
                int <<= 8;
                int += seq[it++];
            }
            data.push(int);
        }
        else if(seq[it]==0x04||seq[it]==0x06) {
            it++;
            let arr_len = seq[it++];
            let res = new Uint8Array(arr_len);
            for(let i=0;i<arr_len;i++) {
                res[i] = seq[it++];
            }
            data.push(res);
        }
        else {
            console.error("ERROR converting DER to list", seq[it]);
            return;
        }
    }
    return data;
}

// parses reserve_key_and_get_half result
function parse_first_result(msg_arr) {
    let data = ASN_DER_to_list(msg_arr);
    return {
        "state_id": data[0],
        "call#": data[1],
        "errors": data[2],
        "key_id": data[3],
        "key_half": data[4],
        "other_hash": data[5],
        "hash_id": data[6]
    };
}

function parse_second_result(msg_arr) {
    let data = ASN_DER_to_list(msg_arr);
    return {
        "state_id": data[0],
        "call#": data[1],
        "errors": data[2],
        "key_half": data[3],
        "other_hash": data[4],
        "hash_id": data[5]
    }
}

function send_asn_request() {
    let socket = new WebSocket("ws://localhost:8080/ws")
    socket.onopen = () => {

        socket.onmessage = async (msg) => {
            const first_msg_arr = new Uint8Array(await msg.data.arrayBuffer())
            const first_res = parse_first_result(first_msg_arr);
            console.log(first_res)

            let key_id = first_res["key_id"];
            socket.onmessage = async (msg) => {
                const second_msg_arr = new Uint8Array(await msg.data.arrayBuffer())
                const second_res = parse_second_result(second_msg_arr);
                console.log(second_res)
            }
            get_key_half(socket, key_id);
        }
        reserve_key_and_get_half(socket)

    }
    socket.onclose = (event) =>  console.log("socket closed connection: ", event)
    socket.onerror = (error) => console.log("socket error: ",error)
}