package api

import (
	"qkdc-service/constants"
	"qkdc-service/encoding"
	"qkdc-service/models"

	"github.com/gorilla/websocket"
	"github.com/jedib0t/go-pretty/text"
)

func (c *Controller) handleSetStateRequest(conn *websocket.Conn, sequence encoding.DERSequence) {
	request, cNonce, err := parseSetStateRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("%v SetState request with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.SetStateRequest), cNonce)
	c.debugLogger.Printf("request cnonce=%v: %+v", cNonce, *request)

	response := c.manager.SetState(request)

	cNonce += 1

	c.infoLogger.Printf("%v SetState response with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.SetStateResponse), cNonce)
	c.debugLogger.Printf("response cnonce=%v: %+v", cNonce, *response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeSetStateResponse(response, cNonce))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseSetStateRequest(sequence encoding.DERSequence) (request *models.SetStateRequest, cNonce int, err error) {
	request = &models.SetStateRequest{}
	err = encoding.DecodeIntoVariables(sequence, nil, &request.State, &request.KeyId0, &request.KeyId1, &cNonce)
	return
}

func encodeSetStateResponse(response *models.SetStateResponse, cNonce int) []byte {
	res := encoding.DERSequence{}
	res = append(res, encoding.CreateIntSeqElement(response.ErrId))
	res = append(res, encoding.CreateIntSeqElement(constants.SetStateResponse))
	res = append(res, encoding.CreateIntSeqElement(cNonce))
	return res.ToByteArray()
}
