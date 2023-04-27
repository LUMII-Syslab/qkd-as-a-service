package api

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"qkdc-service/constants"
	"qkdc-service/encoding"
	"qkdc-service/manager"

	"github.com/gorilla/websocket"
)

type Controller struct {
	infoLogger  *log.Logger
	errorLogger *log.Logger
	debugLogger *log.Logger
	manager     *manager.KeyManager
	upgrader    websocket.Upgrader
}

func NewController(infoLogger *log.Logger, errorLogger *log.Logger, debugLogger *log.Logger, manager *manager.KeyManager) *Controller {
	return &Controller{
		infoLogger:  infoLogger,
		errorLogger: errorLogger,
		debugLogger: debugLogger,
		manager:     manager,
		upgrader: websocket.Upgrader{
			ReadBufferSize:  1024,
			WriteBufferSize: 1024,
			CheckOrigin: func(r *http.Request) bool {
				return true
			},
		},
	}
}

func (c *Controller) ListenAndServe(APIPort int) {
	mux := http.NewServeMux()
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		conn, err := c.upgrader.Upgrade(w, r, nil)
		if err != nil {
			return
		}

		for {
			_, body, err := conn.ReadMessage()
			if err != nil {
				_ = conn.Close()
				break
			}

			sequence, err := encoding.DecodeDERSequence(body)
			if err != nil {
				c.errorLogger.Println(err)
				continue
			}

			if len(sequence) < 1 {
				c.errorLogger.Println("sequence of length zero received")
				continue
			}

			requestId := sequence[0].AsInt()

			switch requestId {
			case constants.ReserveKeyRequest:
				c.handleRKAGHRequest(conn, sequence)
			case constants.GetKeyHalfRequest:
				c.handleGKHRequest(conn, sequence)
			case constants.GetStateRequest:
				c.handleGetStateRequest(conn, sequence)
			case constants.SetStateRequest:
				c.handleSetStateRequest(conn, sequence)
			}
		}
	})

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", APIPort))
	if err != nil {
		log.Panicf("failed to listen: %v", err)
	}

	log.Printf("server listening at %v", lis.Addr())
	log.Panic(http.Serve(lis, mux))
}
