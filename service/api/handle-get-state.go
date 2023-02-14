package api

import (
	"errors"
	"github.com/gorilla/websocket"
	"qkdc-service/constants"
	"qkdc-service/models"
)

func (c *Controller) handleGetStateRequest(conn *websocket.Conn, sequence DERSequence) {
	request, cNonce, err := parseGetStateRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("%#02x request  cNonce=%v: %+v", constants.GetStateRequest, cNonce, *request)
	response := c.manager.GetState(request)

	cNonce += 1

	c.infoLogger.Printf("%#02x response cNonce=%v: %+v", constants.GetStateResponse, cNonce, *response)
	err = conn.WriteMessage(websocket.BinaryMessage, encodeGetStateResponse(response, cNonce))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseGetStateRequest(seq DERSequence) (request *models.GetStateRequest, cNonce int, err error) {
	if len(seq) != 2 {
		err = errors.New("sequence of length 2 was expected")
		return
	}
	cNonce = seq[1].AsInt()
	request = &models.GetStateRequest{}
	return
}

func encodeGetStateResponse(response *models.GetStateResponse, cNonce int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(response.ErrId))
	res = append(res, CreateIntSeqElement(constants.GetStateResponse))
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateIntSeqElement(response.State))
	res = append(res, CreateArrSeqElement(response.KeyId0))
	res = append(res, CreateArrSeqElement(response.KeyId1))
	return res.ToByteArray()
}
