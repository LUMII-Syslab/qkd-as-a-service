package main

import (
	"log"
	"time"

	zmq "github.com/pebbe/zmq4"
)

const ClavisKeyUrl = "tcp://*:5555" //"tcp://192.168.10.101:5560"

func main() {
	zctx, err := zmq.NewContext()
	if err != nil {
		log.Panicln(err)
	}

	zs, err := zctx.NewSocket(zmq.SUB)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("created a new zeromq socket")

	err = zs.Bind(ClavisKeyUrl)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("binded zeromq socket to", ClavisKeyUrl)

	for {
		// Wait for next request from client
		msg, err := zs.Recv(0)
		if err != nil {
			log.Panicln(err)
		}

		log.Printf("Received %s\n", msg)

		// Do some 'work'
		time.Sleep(time.Second * 1)
	}
}
