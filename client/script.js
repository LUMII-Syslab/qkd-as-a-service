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

function reserve_key_and_get_half() {
    const res = new Uint8Array(13)
    res[0] = 0x30; res[1] = 0x0B // a sequence of 11 bytes will follow
    res[2] = 0x02; res[3] = 0x01 // an integer of 1 byte will follow
    res[4] = 0x01 // reserveKeyAndGetKeyHalf request
    res[5] = 0x02; res[6] = 0x02 // an integer of 2 bytes will follow
    res[7] = 0x01; res[8] = 0x00 // requested key length (256)
    res[9] = 0x02; res[10] = 0x02 // an integer of 2 bytes will follow
    res[11] = 0x30; res[12] = 0x39 // call id
    return res
}

function send_asn_request() {
    let socket = new WebSocket("ws://localhost:8080/ws")
    socket.onopen = () => {
        socket.send(reserve_key_and_get_half())
    }
    socket.onclose = (event) =>  console.log("socket closed connection: ", event)
    socket.onerror = (error) => console.log("socket error: ",error)
}