var rkagkh_request;
var rkagkh_err_msg;
var rkagkh_endpoint;

$(() => {
    update_request();
    $('#rkagkh-kdc').change(update_request);
    $('#rkagkh-key-length').change(update_request);
    $('#rkagkh-c-nonce').change(update_request);
    $('#rkagkh-send').click(send_request);
});

function update_request() {
    var kdc = $('#rkagkh-kdc').val();
    if (kdc != "Aija" && kdc != "Brencis") {
        rkagkh_err_msg = "KDC must be either Aija or Brencis";
        return;
    }
    rkagkh_endpoint = kdc == "Aija" ? $("#kdcc-aija-url").val() : $("#kdcc-brencis-url").val();

    var key_length = +$('#rkagkh-key-length').val();
    var c_nonce = +$('#rkagkh-c-nonce').val();
    rkagkh_request = encode_request(key_length, c_nonce)
    if (rkagkh_err_msg) {
        $("#rkagkh-error code").text(rkagkh_err_msg);
        $("#rkagkh-error").show();
        $("#rkagkh-encoded").hide();
        $("#rkagkh-send").prop("disabled", true);
    } else {
        console.log(rkagkh_request)
        $("#rkagkh-encoded code").text(hex_octets(rkagkh_request));
        $("#rkagkh-error").hide();
        $("#rkagkh-encoded").show();
        $("#rkagkh-send").prop("disabled", false);
    }
}

function encode_request(key_length, c_nonce) {
    if (key_length != 256) {
        rkagkh_err_msg = "Key length must be 256";
        return;
    }

    if (c_nonce < 0 || c_nonce > 65535) {
        rkagkh_err_msg = "Crypto nonce must be between 0 and 65535";
        return;
    }

    const req = new Uint8Array(13)
    // sequence
    req[0] = 0x30; req[1] = 0x0B    // a sequence of 11 bytes will follow
    // reserveKeyAndGetKeyHalf request
    req[2] = 0x02; req[3] = 0x01    // an integer of 1 byte will follow
    req[4] = 0x01
    // requested key length (256)
    req[5] = 0x02; req[6] = 0x02    // an integer of 2 bytes will follow
    req[7] = 0x01; req[8] = 0x00
    // crypto nonce
    req[9] = 0x02; req[10] = 0x02   // an integer of 2 bytes will follow
    req[11] = 0x30; req[12] = 0x39

    rkagkh_err_msg = ""; // reset error message

    return req;
}

async function send_request() {
    try {
        let socket = await ws_connect(rkagkh_endpoint);
        let response = await ws_send_request(socket, rkagkh_request);
        console.log(response);
    } catch (error) {
        alert(`websocket connection to ${rkagkh_endpoint} failed`)
    }
}