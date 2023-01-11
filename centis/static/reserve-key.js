var rkagkh_request;
var rkagkh_err_msg;

$(() => {
    var kdc = $('#rkagkh-kdc').val();
    if (kdc != "Aija" && kdc != "Brencis") {
        rkagkh_err_msg = "KDC must be either Aija or Brencis";
        return;
    }
    var key_length = +$('#rkagkh-key-length').val();
    var c_nonce = +$('#rkagkh-c-nonce').val();
    rkagkh_request = encode_request(key_length, c_nonce)
    if (rkagkh_err_msg) {
        $("#rkagkh-error code").text(rkagkh_err_msg);

        $("#rkagkh-error").show();
        $("#rkagkh-encoded").hide();
    } else {
        console.log(rkagkh_request)
        $("#rkagkh-encoded code").text(hex_octets(rkagkh_request));

        $("#rkagkh-error").hide();
        $("#rkagkh-encoded").show();
    }
});

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

    return req;
}

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