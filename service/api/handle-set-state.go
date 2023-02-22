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

	c.infoLogger.Printf("%#02x request  cNonce=%v: %+v", constants.SetStateRequest, cNonce, *request)
	response := c.manager.SetState(request)

	cNonce += 1

	c.infoLogger.Printf("%#02x response cNonce=%v: %+v", constants.SetStateResponse, cNonce, *response)
	err = conn.WriteMessage(websocket.BinaryMessage, encodeSetStateResponse(response, cNonce))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseSetStateRequest(seq DERSequence) (request *models.SetStateRequest, cNonce int, err error) {
	if len(seq) != 5 {
		err = errors.New("sequence of length 5 was expected")
		return
	}
	request = &models.SetStateRequest{}
	_, request.State, request.KeyId0, request.KeyId1, cNonce = seq[0].AsInt(), seq[1].AsInt(), seq[2].AsBytes(), seq[3].AsBytes(), seq[4].AsInt()
	return
}

func encodeSetStateResponse(response *models.SetStateResponse, cNonce int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(response.ErrId))
	res = append(res, CreateIntSeqElement(constants.SetStateResponse))
	res = append(res, CreateIntSeqElement(cNonce))
	return res.ToByteArray()
}
