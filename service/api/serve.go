package api

import (
	"errors"
)

func parseGKHRequest(seq DERSequence) (keyLength int, keyId []byte, callId int, err error) {
	if len(seq) != 4 {
		return 0, nil, 0, errors.New("sequence of length 4 was expected")
	}
	keyLength, keyId, callId = seq[1].AsInt(), seq[2].AsBytes(), seq[3].AsInt()
	return
}

func encodeGKHResponse(cNonce int, errCode int, thisHalf []byte, otherHash []byte, hashAlgId []byte) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(errCode))
	res = append(res, CreateIntSeqElement(0xfe)) // getKeyHalf result
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateArrSeqElement(thisHalf))
	res = append(res, CreateArrSeqElement(otherHash))
	res = append(res, CreateObjSeqElement(hashAlgId))
	return res.ToByteArray()
}

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
