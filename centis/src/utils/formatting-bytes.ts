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

export function bytesToSpacedHexOctets(data: Uint8Array): string {
    let res2 = ""
    // if data is null return null
    if (data === null) {
        return "";
    }
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



export function ASNDERToList(seq): (number | Uint8Array)[] {
    try {
        let it = 2;
        let data = [];
        while (it < 2 + seq[1]) {
            if (seq[it] === 0x02) { // an integer follows
                it++;
                let int_len = seq[it++];
                let int = 0;
                for (let i = 0; i < int_len; i++) {
                    int <<= 8;
                    int += seq[it++];
                }
                data.push(int);
            } else if (seq[it] === 0x04 || seq[it] === 0x06) {
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
    } catch (e) {
        console.error("ERROR converting DER to list", e);
        return;
    }
}

export function hexOctetsToUint8Array(hexOctetStr: string): Uint8Array {
    let res = new Uint8Array(hexOctetStr.length / 2);
    for (let i = 0; i < res.length; i++) {
        res[i] = parseInt(hexOctetStr.substring(i * 2, i * 2 + 2), 16);

    }
    return res;
}