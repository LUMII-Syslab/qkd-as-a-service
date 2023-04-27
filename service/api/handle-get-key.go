package api

import (
	"errors"
	"qkdc-service/constants"
	"qkdc-service/encoding"
	"qkdc-service/models"

	"github.com/gorilla/websocket"
	"github.com/jedib0t/go-pretty/text"
)

// handle request key and get half request
func (c *Controller) handleGKHRequest(conn *websocket.Conn, sequence encoding.DERSequence) {
	request, cNonce, err := parseGKHRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("%v G.K.H. request with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.GetKeyHalfRequest), cNonce)
	c.debugLogger.Printf("request cnonce=%v: %+v", cNonce, *request)

	response := c.manager.GetKeyHalf(request)

	cNonce += 1

	c.infoLogger.Printf("%v G.K.H.response with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.GetKeyHalfResponse), cNonce)
	c.debugLogger.Printf("response cnonce=%v: %+v", cNonce, *response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeGKHResponse(response, cNonce))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseGKHRequest(seq encoding.DERSequence) (request *models.GKHRequest, cNonce int, err error) {
	if len(seq) != 4 {
		err = errors.New("sequence of length 4 was expected")
		return
	}
	request = &models.GKHRequest{}
	request.KeyLength, request.KeyId, cNonce = seq[1].AsInt(), seq[2].AsBytes(), seq[3].AsInt()
	return
}

func encodeGKHResponse(response *models.GKHResponse, cNonce int) []byte {
	res := encoding.DERSequence{}
	res = append(res, encoding.CreateIntSeqElement(response.ErrId))
	res = append(res, encoding.CreateIntSeqElement(constants.GetKeyHalfResponse))
	res = append(res, encoding.CreateIntSeqElement(cNonce))
	res = append(res, encoding.CreateArrSeqElement(response.ThisHalf))
	res = append(res, encoding.CreateArrSeqElement(response.OtherHash))
	res = append(res, encoding.CreateObjSeqElement(response.HashAlgId))
	return res.ToByteArray()
}
