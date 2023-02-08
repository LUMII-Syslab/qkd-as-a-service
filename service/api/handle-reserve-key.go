package api

import (
	"errors"
	"github.com/gorilla/websocket"
	"qkdc-service/constants"
	"qkdc-service/models"
)

// handle request key and get half request
func (c *Controller) handleRKAGHRequest(conn *websocket.Conn, sequence DERSequence) {
	request, cNonce, err := parseRKAGHRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("0x01 request %+v with c nonce %v", request, cNonce)

	response := c.manager.ReserveKeyAndGetHalf(request)

	c.infoLogger.Printf("0x01 response %+v", response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeRKAGHResponse(response, cNonce+1))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseRKAGHRequest(seq DERSequence) (request *models.RKAGHRequest, cNonce int, err error) {
	if len(seq) != 3 {
		err = errors.New("sequence of length 3 was expected")
		return
	}
	request = &models.RKAGHRequest{}
	_, request.KeyLength, cNonce = seq[0].AsInt(), seq[1].AsInt(), seq[2].AsInt()
	return
}

func encodeRKAGHResponse(response *models.RKAGHResponse, cNonce int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(response.ErrId))
	res = append(res, CreateIntSeqElement(constants.RserveKeyAndGetHalfResponse))
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateArrSeqElement(response.KeyId))
	res = append(res, CreateArrSeqElement(response.ThisHalf))
	res = append(res, CreateArrSeqElement(response.OtherHash))
	res = append(res, CreateObjSeqElement(response.HashAlgId))
	return res.ToByteArray()
}