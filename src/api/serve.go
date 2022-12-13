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
			}
			stateId := seq[0].AsInt()
			switch stateId {
			case 0x01: // reserveKeyAndGetHalf
				keyLength := seq[1].AsInt()
				callId := seq[2].AsInt()
				err = conn.WriteMessage(msgType, []byte(manager.ReserveKey()))
			case 0x02: // getKeyHalf
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
