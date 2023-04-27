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
    let result = [];
    try {
        let length = seq[1];
        let start = 2;
        if(seq[1] & 0b10000000) {
            let lengthOfLength = seq[1] & 0b01111111;
            length = 0;
            for (let i = 0; i < lengthOfLength; i++) {
                length <<= 8;
                length += seq[2 + i];
            }
            start = 2 + lengthOfLength;
        }
        for(let it = start; it < start + length;) {
            if (seq[it] === 0x02) { // an integer follows
                it++;
                let int_len = seq[it++];
                let int = 0;
                for (let i = 0; i < int_len; i++) {
                    int <<= 8;
                    int += seq[it++];
                }
                result.push(int);
            } else if (seq[it] === 0x04 || seq[it] === 0x06) {
                it++;
                let arr_len = seq[it++];
                let res = new Uint8Array(arr_len);
                for (let i = 0; i < arr_len; i++) {
                    res[i] = seq[it++];
                }
                result.push(res);
            } else {
                console.error("ERROR converting DER to list", seq[it]);
                return [];
            }
        }
        return result;
    } catch (e) {
        console.error("ERROR converting DER to list", e);
        return [];
    }
}

export function hexOctetsToUint8Array(hexOctetStr: string): Uint8Array {
    let res = new Uint8Array(hexOctetStr.length / 2);
    for (let i = 0; i < res.length; i++) {
        res[i] = parseInt(hexOctetStr.substring(i * 2, i * 2 + 2), 16);

    }
    return res;
}