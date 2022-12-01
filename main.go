package main

import (
	"log"

	zmq "github.com/pebbe/zmq4"
)

const ClavisKeyUrl = "tcp://192.168.10.101:5560"

func main() {
	zctx, err := zmq.NewContext()
	if err != nil {
		log.Panicln(err)
	}

	zs, err := zctx.NewSocket(zmq.SUB)
	if err != nil {
		log.Panicln(err)
	}
	zs.SetSubscribe("")
	defer zs.Close()
	log.Println("created a new zeromq socket")

	log.Println("attempting to connect to", ClavisKeyUrl)
	err = zs.Connect(ClavisKeyUrl)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("connected zeromq to", ClavisKeyUrl)

	for {
		// Wait for next request from client
		msg, err := zs.Recv(0)
		if err != nil {
			log.Panicln(err)
		}

		log.Printf("Received %s\n", msg)
	}
}
