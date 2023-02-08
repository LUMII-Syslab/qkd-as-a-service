package api

func encodeErrResponse(errCode int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(0xfe))    // getKeyHalf result
	res = append(res, CreateIntSeqElement(errCode)) // error
	return res.ToByteArray()
}
