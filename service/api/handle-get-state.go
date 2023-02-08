package api

import (
	"github.com/gorilla/websocket"
)

func (c *Controller) handleGetStateRequest(conn *websocket.Conn, sequence DERSequence) {
	// TODO: implement
	//infoLogger.Println("0x03 sequence: (%v)", sequence)
	//cNonce, err := parseGetStateRequest(sequence)
	//if err != nil {
	//	err = conn.WriteMessage(msgType, encodeErrResponse(constants.ErrorInvalidReq))
	//	errorLogger.Println(err)
	//	continue
	//}
	//
	//infoLogger.Printf("0x03 sequence: (%v)", cNonce)
	//
	//state, keyId0, keyId1, kdcErr := manager.GetState()
	//errCode := 0
	//if kdcErr != nil {
	//	errCode = kdcErr.Code
	//	errorLogger.Println(kdcErr.ToString())
	//}
	//
	//infoLogger.Println("0x03 response c nonce: ", cNonce)
	//infoLogger.Println("0x03 response state: ", state)
	//infoLogger.Println("0x03 response key id 0: ", keyId0)
	//infoLogger.Println("0x03 response key id 1: ", keyId1)
	//
	//err = conn.WriteMessage(msgType, encodeGetStateResponse(cNonce, errCode, state, keyId0, keyId1))
}
