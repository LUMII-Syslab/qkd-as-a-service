package main

import (
	"encoding/hex"
	"fmt"
	"github.com/gorilla/websocket"
	"log"
	"net/http"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

var hashAlgorithmId = []byte{0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x11}

func listenAndServe(manager KeyManager) {
	http.Handle("/", http.FileServer(http.Dir("./client")))
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Println(err)
			return
		}
		for {
			msgType, body, err := conn.ReadMessage() // msgType https://www.rfc-editor.org/rfc/rfc6455.html#section-11.8
			if err != nil {
				//log.Println(err)
				return
			}

			req := body[2 : 2+body[1]] // trim sequence indicator and trailing bytes
			log.Println(fmt.Sprintf("body: %v", hex.EncodeToString(req)))

			addByteArray := func(s int, t byte, res []byte, src []byte) int {
				res[s] = t
				res[s+1] = byte(len(src))
				for i := 0; i < len(src); i++ {
					res[s+2+i] = src[i]
				}
				s += len(src) + 2
				return s
			}

			switch req[2] {
			case 0x01: // reserveKeyAndGetHalf(...)
				keyLength := uint(req[5])*256 + uint(req[6])
				callId := uint(req[9])*256 + uint(req[10])
				keyId, thisHalf, otherHash := manager.reserveKeyAndGetHalf(keyLength)
				resBytes := 2                        // sequence + its length
				resBytes += 3                        // integer + its length + function id (-1 for reserveKeyAndGetKeyHalf result)
				resBytes += 4                        // integer + its length + call id
				resBytes += 3                        // integer + its length + error code (zero for no errors)
				resBytes += 2 + len(keyId)           // byte array + its length + key id
				resBytes += 2 + len(thisHalf)        // byte array + its lenght + this half
				resBytes += 2 + len(otherHash)       // byte array + its lenght + other hash
				resBytes += 2 + len(hashAlgorithmId) // object identifier + its length + hashAlgorithmId
				res := make([]byte, resBytes)
				res[0] = 0x30                  // sequence
				res[1] = byte(resBytes - 2)    // length after first two bytes
				res[2] = 0x02                  // integer
				res[3] = 0x01                  // length of integer
				res[4] = 0xff                  // reserveKeyAndGetHalf result (-1)
				res[5] = 0x02                  // integer
				res[6] = 0x02                  // length of integer
				res[7] = byte(callId & 0xff00) // callId first part
				res[8] = byte(callId & 0x00ff) // callId second part
				res[9] = 0x02                  // integer
				res[10] = 0x01                 // length of integer
				res[11] = 0x00                 // error code (zero for now)
				s := 12
				s = addByteArray(s, 0x04, res, keyId)
				s = addByteArray(s, 0x04, res, thisHalf)
				s = addByteArray(s, 0x04, res, otherHash)
				s = addByteArray(s, 0x06, res, hashAlgorithmId)
				err = conn.WriteMessage(msgType, res)
			case 0x02: // getKeyHalf(id, ...)
				err = conn.WriteMessage(msgType, []byte(manager.reserveKey()))
			}
			if err != nil {
				log.Println(err)
				return
			}

		}
	})
	log.Panic(http.ListenAndServe("localhost:8080", nil))
}
