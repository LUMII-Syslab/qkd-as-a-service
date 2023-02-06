package api

import (
	"errors"
	"fmt"
	"github.com/gorilla/websocket"
	"log"
	"net"
	"net/http"
	"qkdc-service/constants"
	"qkdc-service/manager"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

var hashAlgId = []byte{0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x11}

func ListenAndServe(manager *manager.KeyManager, infoLogger *log.Logger, errorLogger *log.Logger, APIPort int) {
	mux := http.NewServeMux()
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			errorLogger.Println(err)
			return
		}

		for {
			msgType, body, err := conn.ReadMessage() // msgType https://www.rfc-editor.org/rfc/rfc6455.html#section-11.8
			if err != nil {
				errorLogger.Println(err)
				_ = conn.Close()
				break
			}
			seq, err := DecodeDERSequence(body)
			if err != nil {
				errorLogger.Println(err)
				continue
			}
			stateId := seq[0].AsInt()

			switch stateId {

			case constants.RKAGHRequest: // reserveKeyAndGetHalf
				keyLength, cNonce, err := parseRKAGHRequest(seq)
				if err != nil {
					errorLogger.Println(err)
					continue
				}

				infoLogger.Println("0x01 request key length: ", keyLength)
				infoLogger.Println("0x01 request c nonce: ", cNonce)

				keyId, val, hash, kdcErr := manager.ReserveKeyAndGetHalf()
				errCode := 0
				if kdcErr != nil {
					errCode = kdcErr.Code
					errorLogger.Println(kdcErr.ToString())
				}

				infoLogger.Println("0x01 response c nonce: ", cNonce)
				infoLogger.Println("0x01 response err code:", errCode)
				infoLogger.Println("0x01 response key id:", keyId)
				infoLogger.Println("0x01 response this half", val)
				infoLogger.Println("0x01 response other hash", hash)

				err = conn.WriteMessage(msgType, encodeRKAGHResponse(cNonce, errCode, keyId, val, hash, hashAlgId))

			case constants.GKHRequest: // getKeyHalf
				keyLength, keyId, cNonce, err := parseGKHRequest(seq)
				if err != nil {
					errorLogger.Println(err)
					continue
				}

				infoLogger.Println("0x02 request: (%v), (%v), (%v)", keyLength, cNonce, keyId)

				thisHalf, otherHash, kdcErr := manager.GetKeyThisHalfOtherHash(keyId)
				errCode := 0
				if kdcErr != nil {
					errCode = kdcErr.Code
					errorLogger.Println(kdcErr.ToString())
				}

				infoLogger.Println("0x02 response c nonce: ", cNonce)
				infoLogger.Println("0x02 response err code:", errCode)
				infoLogger.Println("0x02 response this half", thisHalf)
				infoLogger.Println("0x02 response other hash", otherHash)

				err = conn.WriteMessage(msgType, encodeGKHResponse(cNonce, errCode, thisHalf, otherHash, hashAlgId))
			case 0x03: // getState
				infoLogger.Println("0x03 request: ", seq.ToString())
				if len(seq) != 0 {
					log.Println("sequence of length 0 was expected")
					err = conn.WriteMessage(msgType, encodeErrResponse(1))
					continue
				}
				res := DERSequence{}
				err = conn.WriteMessage(msgType, res.ToByteArray())
			case 0x04: // setState
				infoLogger.Println("0x04 request: ", seq.ToString())
				if len(seq) != 0 {
					log.Println("sequence of length 0 was expected")
					continue
				}
				res := DERSequence{}
				err = conn.WriteMessage(msgType, res.ToByteArray())
			}

			if err != nil {
				errorLogger.Println(err)
				continue
			}
		}
	})

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", APIPort))
	if err != nil {
		errorLogger.Panicf("failed to listen: %v", err)
	}
	log.Printf("server listening at %v", lis.Addr())
	log.Panic(http.Serve(lis, mux))
}

func parseRKAGHRequest(seq DERSequence) (keyLength int, callId int, err error) {
	if len(seq) != 3 {
		return 0, 0, errors.New("sequence of length 3 was expected")
	}
	keyLength, callId = seq[1].AsInt(), seq[2].AsInt()
	return
}

func encodeRKAGHResponse(cNonce int, errCode int, keyId []byte, thisHalf []byte, otherHash []byte, hashAlgId []byte) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(0xff)) // reserveKeyAndGetHalf result
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateIntSeqElement(errCode))
	res = append(res, CreateArrSeqElement(keyId))
	res = append(res, CreateArrSeqElement(thisHalf))
	res = append(res, CreateArrSeqElement(otherHash))
	res = append(res, CreateObjSeqElement(hashAlgId))
	return res.ToByteArray()
}

func parseGKHRequest(seq DERSequence) (keyLength int, keyId []byte, callId int, err error) {
	if len(seq) != 4 {
		return 0, nil, 0, errors.New("sequence of length 4 was expected")
	}
	keyLength, keyId, callId = seq[1].AsInt(), seq[2].AsBytes(), seq[3].AsInt()
	return
}

func encodeGKHResponse(cNonce int, errCode int, thisHalf []byte, otherHash []byte, hashAlgId []byte) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(0xfe)) // getKeyHalf result
	res = append(res, CreateIntSeqElement(cNonce))
	res = append(res, CreateIntSeqElement(errCode))
	res = append(res, CreateArrSeqElement(thisHalf))
	res = append(res, CreateArrSeqElement(otherHash))
	res = append(res, CreateObjSeqElement(hashAlgId))
	return res.ToByteArray()
}

func encodeErrResponse(errCode int) []byte {
	res := DERSequence{}
	res = append(res, CreateIntSeqElement(0xfe))    // getKeyHalf result
	res = append(res, CreateIntSeqElement(errCode)) // error
	return res.ToByteArray()
}
