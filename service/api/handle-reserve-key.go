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
func (c *Controller) handleRKAGHRequest(conn *websocket.Conn, sequence encoding.DERSequence) {
	request, cNonce, err := parseRKAGHRequest(sequence)
	if err != nil {
		c.errorLogger.Println(err)
		return
	}

	c.infoLogger.Printf("%v R.K.A.G.H. request with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.ReserveKeyRequest), cNonce)
	c.debugLogger.Printf("request cnonce=%v: %+v", cNonce, *request)

	response := c.manager.ReserveKeyAndGetHalf(request)

	cNonce += 1

	c.infoLogger.Printf("%v R.K.A.G.H. response with cnonce=%v", text.FgGreen.Sprintf("%#02x", constants.ReserveKeyResponse), cNonce)
	c.debugLogger.Printf("response cnonce=%v: %+v", cNonce, *response)

	err = conn.WriteMessage(websocket.BinaryMessage, encodeRKAGHResponse(response, cNonce))
	if err != nil {
		c.errorLogger.Println(err)
	}
}

func parseRKAGHRequest(seq encoding.DERSequence) (request *models.RKAGHRequest, cNonce int, err error) {
	if len(seq) != 3 {
		err = errors.New("sequence of length 3 was expected")
		return
	}
	request = &models.RKAGHRequest{}
	_, request.KeyLength, cNonce = seq[0].AsInt(), seq[1].AsInt(), seq[2].AsInt()
	return
}

func encodeRKAGHResponse(response *models.RKAGHResponse, cNonce int) []byte {
	res := encoding.DERSequence{}
	res = append(res, encoding.CreateIntSeqElement(response.ErrId))
	res = append(res, encoding.CreateIntSeqElement(constants.ReserveKeyResponse))
	res = append(res, encoding.CreateIntSeqElement(cNonce))
	res = append(res, encoding.CreateArrSeqElement(response.KeyId))
	res = append(res, encoding.CreateArrSeqElement(response.ThisHalf))
	res = append(res, encoding.CreateArrSeqElement(response.OtherHash))
	res = append(res, encoding.CreateObjSeqElement(response.HashAlgId))
	return res.ToByteArray()
}
