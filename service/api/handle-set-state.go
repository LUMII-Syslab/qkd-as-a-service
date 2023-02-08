package api

import "github.com/gorilla/websocket"

func (c *Controller) handleSetStateRequest(conn *websocket.Conn, sequence DERSequence) {
	// TODO: implement
	//infoLogger.Println("0x04 sequence: (%v)", sequence)
	//state, keyId0, keyId1, cNonce, err := parseSetStateRequest(sequence)
	//if err != nil {
	//	err = conn.WriteMessage(msgType, encodeErrResponse(constants.ErrorInvalidReq))
	//	errorLogger.Println(err)
	//	continue
	//}
	//
	//infoLogger.Println("0x04 sequence: (%v)", state)
	//infoLogger.Println("0x04 sequence: (%v)", keyId0)
	//infoLogger.Println("0x04 sequence: (%v)", keyId1)
	//infoLogger.Println("0x04 sequence: (%v)", cNonce)
	//
	//kdcErr := manager.SetState(state, keyId0, keyId1)
	//errCode := 0
	//if kdcErr != nil {
	//	errCode = kdcErr.Code
	//	errorLogger.Println(kdcErr.ToString())
	//}
	//
	//infoLogger.Println("0x04 response c nonce: ", cNonce)
	//infoLogger.Println("0x04 response err code:", errCode)
	//
	//err = conn.WriteMessage(msgType, encodeSetStateResponse(cNonce, errCode))
}
