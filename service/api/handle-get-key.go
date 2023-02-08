package api

import (
	"errors"
	"github.com/gorilla/websocket"
	"qkdc-service/constants"
	"qkdc-service/models"
)

// handle request key and get half request
func (c *Controller) handleGKHRequest(conn *websocket.Conn, sequence DERSequence) {
	request, cNonce, err := parseGKHRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("0x01 request %+v with c nonce %v", request, cNonce)

	response := c.manager.GetKeyHalf(request)

	c.infoLogger.Printf("0x01 response %+v", response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeGKHResponse(response, cNonce+1))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseGKHRequest(seq DERSequence) (request *models.GKHRequest, cNonce int, err error) {
	if len(seq) != 4 {
		err = errors.New("sequence of length 4 was expected")
		return
	}
	request = &models.GKHRequest{}
	request.KeyLength, request.KeyId, cNonce = seq[1].AsInt(), seq[2].AsBytes(), seq[3].AsInt()
	return
}

func encodeGKHResponse(response *models.GKHResponse, cNonce int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(response.ErrId))
	res = append(res, CreateIntSeqElement(constants.GetKeyHalfResponse))
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateArrSeqElement(response.ThisHalf))
	res = append(res, CreateArrSeqElement(response.OtherHash))
	res = append(res, CreateObjSeqElement(response.HashAlgId))
	return res.ToByteArray()
}
