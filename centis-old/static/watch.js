var table_size = 3
var watching_keys = false
var aija_watch_ws = null;
var brencis_watch_ws = null;

$(() => {
    $("#toggle-monitor").click(toggle_monitor);

    let fetching = 0
    setInterval(async () => {
        if (watching_keys) {
            try {
                if (fetching !== 0) return;
                fetching++;
                let key = await fetch_key();
                add_key_to_table(key);
                fetching--;
            } catch (e) {
                console.error("error: ", e);
            }
        } else {
            fetching = 0;
        }
    }, 50);
});

async function toggle_monitor() {
    watching_keys = !watching_keys
    if (watching_keys) {
        aija_watch_ws = await ws_connect($("#kdcc-aija-url").val())
        brencis_watch_ws = await ws_connect($("#kdcc-brencis-url").val())
    } else {
        aija_watch_ws.close();
        aija_watch_ws = null;
        brencis_watch_ws.close();
        brencis_watch_ws = null;
    }
}

async function fetch_key() {
    let result = {
        key_id: null,
        key_left: null,
        key_right: null,
        hash_left: null,
        hash_right: null,
    }
    let rkagkh_raw = await ws_send_request(aija_watch_ws, encode_rkagkh_request(256, 0))
    let rkagkh_resp = parse_rkagkh_result(rkagkh_raw)
    result.key_id = rkagkh_resp.key_id
    result.key_left = rkagkh_resp.key_half
    result.hash_right = rkagkh_resp.other_hash
    let key_id_str = hex_octets(rkagkh_resp.key_id)
    let gkh_raw = await ws_send_request(brencis_watch_ws, encode_gkh_request(256, key_id_str, 0))
    let gkh_resp = parse_gkh_result(gkh_raw)
    result.key_right = gkh_resp.key_half
    result.hash_left = gkh_resp.other_hash
    return result;
}

function add_key_to_table(key) {
    let table = document.getElementById("my-table")
    let row = table.insertRow(0)
    key_id = spaced_hex_octets(key.key_id)
    key_left = spaced_hex_octets(key.key_left)
    key_right = spaced_hex_octets(key.key_right)
    hash_left = spaced_hex_octets(key.hash_left)
    hash_right = spaced_hex_octets(key.hash_right)
    row.innerHTML = `<td>${key_id}</td><td>${key_left}</td><td>${key_right}</td><td>${hash_left}</td><td>${hash_right}</td>`
    if (table.rows.length > table_size)
        table.rows[table_size].remove()
}