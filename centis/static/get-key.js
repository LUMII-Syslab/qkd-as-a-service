var gkh_request;
var gkh_err_msg;
var gkh_endpoint;

$(() => {
    update_gkh_request();
    $('#gkh-kdc').change(update_gkh_request);
    $('#gkh-key-length').change(update_gkh_request);
    $('#gkh-key-id').change(update_gkh_request);
    $('#gkh-c-nonce').change(update_gkh_request);
    $('#gkh-send').click(send_gkh_request);
});

function update_gkh_request() {
    var kdc = $('#gkh-kdc').val();
    if (kdc !== "Aija" && kdc !== "Brencis") {
        gkh_err_msg = "KDC must be either Aija or Brencis";
        return;
    }
    gkh_endpoint = kdc === "Aija" ? $("#kdcc-aija-url").val() : $("#kdcc-brencis-url").val();

    var key_length = +$('#gkh-key-length').val();
    var key_id = $('#gkh-key-id').val();
    var c_nonce = +$('#gkh-c-nonce').val();
    // key_length, key_id, c_nonce will be validated by encode_gkh_request
    gkh_request = encode_gkh_request(key_length, key_id, c_nonce)

    if (gkh_err_msg) {
        console.error(gkh_err_msg);
        $("#gkh-error code").text(gkh_err_msg);
        $("#gkh-error").show();
        $("#gkh-encoded").hide();
        $("#gkh-send").prop("disabled", true);
    } else {
        console.log(gkh_request)
        $("#gkh-encoded code").text(spaced_hex_octets(gkh_request));
        $("#gkh-error").hide();
        $("#gkh-encoded").show();
        $("#gkh-send").prop("disabled", false);
    }
}

function encode_gkh_request(key_length, key_id_str, c_nonce) {
    if (key_length !== 256) {
        gkh_err_msg = "Key length must be 256";
        return;
    }

    if (c_nonce < 0 || c_nonce > 65535) {
        gkh_err_msg = "Crypto nonce must be between 0 and 65535";
        return;
    }

    // test if key_id matches ^[0-9A-F]+$
    if (!/^[0-9A-F]+$/i.test(key_id_str)) {
        gkh_err_msg = "Key ID must be a hexadecimal number";
        return;
    }

    let key_id = hex_octets_to_array(key_id_str);

    const req = new Uint8Array(15 + key_id.length)
    // sequence
    req[0] = 0x30;
    req[1] = 13 + key_id.length    // a sequence of 13+len(key_id) bytes will follow

    // getKeyHalf request
    req[2] = 0x02;
    req[3] = 0x01    // an integer of 1 byte will follow
    req[4] = 0x02

    // key length
    req[5] = 0x02;
    req[6] = 0x02    // an integer of 2 bytes will follow
    req[7] = 0x01;
    req[8] = 0x00    // requested key length (256)

    // key id
    req[9] = 0x04;
    req[10] = key_id.length  // a byte array of key_id.length bytes will follow
    for (let i = 0; i < key_id.length; i++)
        req[i + 11] = key_id[i];

    // call#
    req[11 + key_id.length] = 0x02;
    req[12 + key_id.length] = 0x02   // an integer of 2 bytes will follow
    req[13 + key_id.length] = c_nonce >> 8;
    req[14 + key_id.length] = c_nonce % 256

    return req;
}

function parse_gkh_result(msg_arr) {
    let data = ASN_DER_to_list(msg_arr);
    return {
        "state_id": data[0],
        "call#": data[1],
        "errors": data[2],
        "key_half": data[3],
        "other_hash": data[4],
        "hash_id": data[5]
    };
}

async function send_gkh_request() {
    try {
        let socket = await ws_connect(gkh_endpoint);
        let response = await ws_send_request(socket, gkh_request);
        let parsed = parse_gkh_result(response);
        console.log(parsed);
        if (parsed["errors"] !== 0) {
            $("#gkh-resp-c-nonce").text(parsed["call#"]);
            $("#gkh-resp-err-code").text(parsed["errors"]);
            $("#gkh-resp-this-half").text("?");
            $("#gkh-resp-other-hash").text("?");
            $("#gkh-resp-hash-alg-id").text("?");
        } else {
            $("#gkh-resp-c-nonce").text(parsed["call#"]);
            $("#gkh-resp-err-code").text(parsed["errors"]);
            $("#gkh-resp-this-half").text(hex_octets(parsed["key_half"]));
            $("#gkh-resp-other-hash").text(hex_octets(parsed["other_hash"]));
            $("#gkh-resp-hash-alg-id").text(hex_octets(parsed["hash_id"]));
        }
    } catch (error) {
        alert(`websocket connection to ${gkh_endpoint} failed`)
    }
}