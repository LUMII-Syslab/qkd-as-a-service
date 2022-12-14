package data

import (
	"crypto/rand"
	"fmt"
	"log"
	math_rand "math/rand"
	"time"

	zmq "github.com/pebbe/zmq4"
	"github.com/vmihailenco/msgpack/v5"
)

type KeyGatherer struct {
	subscribers []KeyManager
}

func InitKeyGatherer() KeyGatherer {
	return KeyGatherer{make([]KeyManager, 0)}
}

func (kg *KeyGatherer) Subscribe(manager KeyManager) {
	kg.subscribers = append(kg.subscribers, manager)
}

func (kg *KeyGatherer) Start(url string) error {
	if url != "" {
		return kg.gatherClavisKeys(url)
	} else {
		return kg.gatherRandomKeys()
	}
}

func (kg *KeyGatherer) distributeKey(keyId, keyVal []byte) error {
	fmt.Printf("\tk: %v \r", keyVal)
	for _, v := range kg.subscribers {
		err := v.add(keyId, keyVal)
		if err != nil {
			return err
		}
	}
	return nil
}

func (kg *KeyGatherer) gatherClavisKeys(url string) error {
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

	log.Println("attempting to connect to", url)
	err = zs.Connect(url)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("connected zeromq to", url)

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

		kg.distributeKey([]byte(keyId), keyVal)
	}
	return nil
}

func generateRandomBytes(n int) ([]byte, error) {
	b := make([]byte, n)
	rand.Read(b)
	return b, nil
}

func (kg *KeyGatherer) gatherRandomKeys() error {
	for {
		keyId, keyVal := make([]byte, 0), make([]byte, 0)
		rand.Read(keyId)
		rand.Read(keyVal)
		log.Println(keyId, keyVal)
		err := kg.distributeKey(keyId, keyVal)
		if err != nil {
			return err
		}
		time.Sleep(time.Duration(math_rand.Float32() * 3000000000))
	}
}
