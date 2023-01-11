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