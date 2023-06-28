package gatherers

import (
	"encoding/base64"
	"log"

	zmq "github.com/pebbe/zmq4"
	"github.com/vmihailenco/msgpack/v5"
)

type ZeroMqKeyGatherer struct {
	keyGathererBase
	zeroMqUri string
}

func NewZeroMqKeyGatherer(clavisURL string) *ZeroMqKeyGatherer {
	return &ZeroMqKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, zeroMqUri: clavisURL}
}

func (kg *ZeroMqKeyGatherer) Start() error {
	zCtx, err := zmq.NewContext()
	if err != nil {
		log.Panicln(err)
	}

	zs, err := zCtx.NewSocket(zmq.SUB)
	if err != nil {
		log.Panicln(err)
	}
	err = zs.SetSubscribe("")
	if err != nil {
		log.Panicln(err)
	}
	defer func() { err = zs.Close() }()
	log.Println("created a new zeromq socket")

	log.Println("attempting to connect to", kg.zeroMqUri)
	err = zs.Connect(kg.zeroMqUri)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("connected zeromq to", kg.zeroMqUri)

	for {
		msgB, err := zs.RecvBytes(0)
		if err != nil {
			log.Panic(err)
		}
		log.Println("received message from clavis")
		var msg []interface{}
		err = msgpack.Unmarshal(msgB, &msg)
		if err != nil {
			log.Panic(err)
		}
		if len(msg) != 2 {
			log.Println("skipping message with invalid length")
			continue
		}

		var keyId string
		switch t := msg[0].(type) {
		case string:
			keyId = t
		}

		var keyVal []uint8

		switch t := msg[1].(type) {
		case []interface{}:
			for _, v := range t {
				switch t2 := v.(type) {
				case int8:
					keyVal = append(keyVal, uint8(t2))
				case uint8:
					keyVal = append(keyVal, t2)
				}
			}
		}

		encodedString := base64.StdEncoding.EncodeToString(keyVal)
		log.Println("distributing key", keyId, encodedString)

		err = kg.distributeKey([]byte(keyId), keyVal)
		if err != nil {
			return err
		}
	}
}
