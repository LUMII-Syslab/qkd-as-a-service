package gatherer

import (
	zmq "github.com/pebbe/zmq4"
	"github.com/vmihailenco/msgpack/v5"
	"log"
)

type ClavisKeyGatherer struct {
	keyGathererBase
	clavisURL string
}

func NewClavisKeyGatherer(clavisURL string) KeyGatherer {
	return &ClavisKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, clavisURL: clavisURL}
}

func (kg *ClavisKeyGatherer) Start() error {
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

	log.Println("attempting to connect to", kg.clavisURL)
	err = zs.Connect(kg.clavisURL)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("connected zeromq to", kg.clavisURL)

	for i := 0; i < 10; i++ {
		msgB, err := zs.RecvBytes(0)
		if err != nil {
			log.Panic(err)
		}
		var msg []interface{}
		err = msgpack.Unmarshal(msgB, &msg)
		if err != nil {
			log.Panic(err)
		}
		if len(msg) != 2 {
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

		err = kg.distributeKey([]byte(keyId), keyVal)
		if err != nil {
			return err
		}
	}
	return nil
}
