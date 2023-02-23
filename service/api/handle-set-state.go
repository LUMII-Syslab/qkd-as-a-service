package api

import (
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

func parseSetStateRequest(sequence DERSequence) (request *models.SetStateRequest, cNonce int, err error) {
	request = &models.SetStateRequest{}
	err = DecodeIntoVariables(sequence, nil, &request.State, &request.KeyId0, &request.KeyId1, &cNonce)
	return
}

func encodeSetStateResponse(response *models.SetStateResponse, cNonce int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(response.ErrId))
	res = append(res, CreateIntSeqElement(constants.SetStateResponse))
	res = append(res, CreateIntSeqElement(cNonce))
	return res.ToByteArray()
}
