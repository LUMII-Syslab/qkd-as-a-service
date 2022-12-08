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

    socket.onclose = (event) => {
        console.log("socket closed connection: ", event)
    }

    socket.onerror = (error) => {
        console.log("socket error: ",error)
    }
}

function stop_watching_keys() {
    watch_keys = false
    watching_keys = false
}