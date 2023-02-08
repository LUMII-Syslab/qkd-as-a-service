package api

import (
	"errors"
)

func encodeErrResponse(errCode int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(0xfe))    // getKeyHalf result
	res = append(res, CreateIntSeqElement(errCode)) // error
	return res.ToByteArray()
}

func parseGetStateRequest(seq DERSequence) (cNonce int, err error) {
	if len(seq) != 1 {
		err = errors.New("sequence of length 1 was expected")
		return
	}
	cNonce = seq[1].AsInt()
	return
}

func encodeGetStateResponse(cNonce int, errCode int, state int, keyId0 []byte, keyId1 []byte) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(errCode))
	res = append(res, CreateIntSeqElement(0xfd)) // getState result
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateIntSeqElement(state))
	res = append(res, CreateArrSeqElement(keyId0))
	res = append(res, CreateArrSeqElement(keyId1))
	return res.ToByteArray()
}
