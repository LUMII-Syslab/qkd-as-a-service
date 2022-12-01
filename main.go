package main

import (
	"log"

	zmq "github.com/pebbe/zmq4"
	"github.com/vmihailenco/msgpack/v5"
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
		msg_b, err := zs.RecvBytes(0)
		if err != nil {
			log.Panic(err)
		}
		var msg []interface{}
		err = msgpack.Unmarshal(msg_b, &msg)
		if err != nil {
			log.Panic(err)
		}
		if len(msg) != 2 {
			continue
		}
		log.Printf("message: %v\n", msg)
	}
}
