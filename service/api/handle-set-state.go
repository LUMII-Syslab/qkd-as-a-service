package api

import (
	"errors"
	"github.com/gorilla/websocket"
	"qkdc-service/constants"
	"qkdc-service/models"
)

func (c *Controller) handleSetStateRequest(conn *websocket.Conn, sequence DERSequence) {
	request, cNonce, err := parseSetStateRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("0x03 request with c nonce %v", cNonce)

	response := c.manager.SetState(request)

	c.infoLogger.Printf("0x03 response %+v", response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeSetStateResponse(response, cNonce+1))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseSetStateRequest(seq DERSequence) (request *models.SetStateRequest, cNonce int, err error) {
	if len(seq) != 2 {
		err = errors.New("sequence of length 2 was expected")
		return
	}
	cNonce = seq[1].AsInt()
	return
}

func encodeSetStateResponse(response *models.SetStateResponse, cNonce int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(response.ErrId))
	res = append(res, CreateIntSeqElement(constants.SetStateResponse))
	res = append(res, CreateIntSeqElement(cNonce))
	return res.ToByteArray()
}
