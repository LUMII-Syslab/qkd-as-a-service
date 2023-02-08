package api

import "github.com/gorilla/websocket"

// handle request key and get half request
func (c *Controller) handleGKHRequest(conn *websocket.Conn, sequence DERSequence) {
	// TODO: implement
	//keyLength, keyId, cNonce, err := parseGKHRequest(sequence)
	//if err != nil {
	//
	//	errorLogger.Println(err)
	//	continue
	//}
	//
	//infoLogger.Println("0x02 sequence: (%v), (%v), (%v)", keyLength, cNonce, keyId)
	//
	//thisHalf, otherHash, kdcErr := manager.GetKeyThisHalfOtherHash(keyId)
	//errCode := 0
	//if kdcErr != nil {
	//	errCode = kdcErr.Code
	//	errorLogger.Println(kdcErr.ToString())
	//}
	//
	//infoLogger.Println("0x02 response c nonce: ", cNonce)
	//infoLogger.Println("0x02 response err code:", errCode)
	//infoLogger.Println("0x02 response this half", thisHalf)
	//infoLogger.Println("0x02 response other hash", otherHash)
	//
	//err = conn.WriteMessage(msgType, encodeGKHResponse(cNonce, errCode, thisHalf, otherHash, hashAlgId))

}
