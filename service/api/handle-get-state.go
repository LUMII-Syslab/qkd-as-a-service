package api

import (
	"errors"
	"qkdc-service/constants"
	"qkdc-service/encoding"
	"qkdc-service/models"

	"github.com/gorilla/websocket"
	"github.com/jedib0t/go-pretty/text"
)

func (c *Controller) handleGetStateRequest(conn *websocket.Conn, sequence encoding.DERSequence) {
	request, cNonce, err := parseGetStateRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("%v GetState request with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.GetStateRequest), cNonce)
	c.debugLogger.Printf("request cnonce=%v: %+v", cNonce, *request)

	response := c.manager.GetState(request)

	cNonce += 1

	c.infoLogger.Printf("%v GetState response with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.GetStateResponse), cNonce)
	c.debugLogger.Printf("response cnonce=%v: %+v", cNonce, *response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeGetStateResponse(response, cNonce))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseGetStateRequest(seq encoding.DERSequence) (request *models.GetStateRequest, cNonce int, err error) {
	if len(seq) != 2 {
		err = errors.New("sequence of length 2 was expected")
		return
	}
	cNonce = seq[1].AsInt()
	request = &models.GetStateRequest{}
	return
}

func encodeGetStateResponse(response *models.GetStateResponse, cNonce int) []byte {
	res := encoding.DERSequence{}
	res = append(res, encoding.CreateIntSeqElement(response.ErrId))
	res = append(res, encoding.CreateIntSeqElement(constants.GetStateResponse))
	res = append(res, encoding.CreateIntSeqElement(cNonce))
	res = append(res, encoding.CreateIntSeqElement(response.State))
	res = append(res, encoding.CreateIntSeqElement(response.KeysStored))
	res = append(res, encoding.CreateIntSeqElement(response.Reservable))
	res = append(res, encoding.CreateIntSeqElement(response.KeysServed))
	res = append(res, encoding.CreateIntSeqElement(response.KeysAdded))
	res = append(res, encoding.CreateArrSeqElement(response.KeyId0))
	res = append(res, encoding.CreateArrSeqElement(response.KeyId1))

	return res.ToByteArray()
}
