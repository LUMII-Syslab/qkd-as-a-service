package main

import (
	"encoding/hex"
	"errors"
	"fmt"
	"github.com/gorilla/websocket"
	"log"
	"net/http"
	"strings"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func listenAndServe(manager KeyManager) {
	http.Handle("/", http.FileServer(http.Dir("./client")))
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Println(err)
			return
		}
		for {
			msgType, body, err := conn.ReadMessage()
			if err != nil {
				//log.Println(err)
				return
			}

			//log.Println(fmt.Sprintf("msgType: %v", msgType), fmt.Sprintf("body: %v", string(body)))
			log.Println(fmt.Sprintf("msgType: %v", msgType), fmt.Sprintf("body: %v", hex.EncodeToString(body)))
			//req := body[:2+body[1]]

			args := strings.Fields(string(body))
			action := args[0]
			params := args[1:]

			switch action {
			case "reserve":
				err = conn.WriteMessage(msgType, []byte(manager.reserveKey()))
			case "get_left":
				if len(params) < 1 {
					err = errors.New("missing parameter")
					break
				}
				var res string
				res, err = manager.getKeyLeft(params[0])
				if err != nil {
					break
				}
				err = conn.WriteMessage(msgType, []byte(res))
			default:
				err = conn.WriteMessage(msgType, body)
			}

			if err != nil {
				log.Println(err)
				return
			}

		}
	})
	log.Panic(http.ListenAndServe("localhost:8080", nil))
}
