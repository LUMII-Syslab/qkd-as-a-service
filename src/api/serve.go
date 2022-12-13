package api

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"qkdc-service/src/data"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

var hashAlgorithmId = []byte{0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x11}

func ListenAndServe(manager data.KeyManager, APIPort int) {
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
				return
			}
			seq, err := DecodeDERSequence(body)
			if err != nil {
				log.Println(err)
				return
			}
			log.Println("request: ", seq.ToString())

			req := body[2 : 2+body[1]] // trim sequence indicator and trailing bytes
			//log.Printf("body: %v\n", hex.EncodeToString(req))

			addByteArray := func(strt int, res []byte, typ byte, src []byte) int {
				res[strt] = typ
				res[strt+1] = byte(len(src))
				for i := 0; i < len(src); i++ {
					res[strt+2+i] = src[i]
				}
				strt += len(src) + 2
				return strt
			}

			addInteger := func(strt int, res []byte, val uint64, len int) int {
				res[strt] = 0x02
				res[strt+1] = byte(len)
				for i := 0; i < len; i++ {
					x := val >> ((len - i - 1) * 8)
					res[strt+2+i] = byte(x)
				}
				strt += len + 2
				return strt
			}

			switch req[2] {
			case 0x01: // reserveKeyAndGetHalf(...)
				keyLength := uint(req[5])*256 + uint(req[6])
				callId := uint(req[9])*256 + uint(req[10])
				keyId, thisHalf, otherHash := manager.ReserveKeyAndGetHalf(keyLength)
				errCode, funcId := 0, 0xff
				resBytes := 2                        // sequence + its length
				resBytes += 3                        // integer + its length + function id (-1 for reserveKeyAndGetKeyHalf result)
				resBytes += 4                        // integer + its length + call id
				resBytes += 3                        // integer + its length + error code (zero for no errors)
				resBytes += 2 + len(keyId)           // byte array + its length + key id
				resBytes += 2 + len(thisHalf)        // byte array + its lenght + this half
				resBytes += 2 + len(otherHash)       // byte array + its lenght + other hash
				resBytes += 2 + len(hashAlgorithmId) // object identifier + its length + hashAlgorithmId
				res := make([]byte, resBytes)
				res[0] = 0x30               // sequence
				res[1] = byte(resBytes - 2) // length after first two bytes
				s := 2
				s = addInteger(s, res, uint64(funcId), 1)  // reserveKeyAndGetHalf result (-1)
				s = addInteger(s, res, uint64(callId), 2)  // callId
				s = addInteger(s, res, uint64(errCode), 1) // error code (zero for now)
				s = addByteArray(s, res, 0x04, keyId)
				s = addByteArray(s, res, 0x04, thisHalf)
				s = addByteArray(s, res, 0x04, otherHash)
				s = addByteArray(s, res, 0x06, hashAlgorithmId)
				err = conn.WriteMessage(msgType, res)
			case 0x02: // getKeyHalf(id, ...)
				log.Println("getKeyHalf req received")
				err = conn.WriteMessage(msgType, []byte(manager.ReserveKey()))
			}
			if err != nil {
				log.Println(err)
				return
			}

		}
	})

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", APIPort))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	log.Printf("server listening at %v", lis.Addr())
	log.Panic(http.Serve(lis, http.DefaultServeMux))
}
