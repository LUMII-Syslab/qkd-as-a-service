package data

import (
	"crypto/rand"
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"github.com/vmihailenco/msgpack/v5"
	"log"
	"qkdc-service/utils"
	"sync"
	"time"
)

type KeyGatherer struct {
	subscribers []*KeyManager
}

func InitKeyGatherer() KeyGatherer {
	return KeyGatherer{make([]*KeyManager, 0)}
}

func (kg *KeyGatherer) Subscribe(manager *KeyManager) {
	kg.subscribers = append(kg.subscribers, manager)
}

var addedKeys = 0

func (kg *KeyGatherer) Start(url string) error {
	go func() {
		for {
			time.Sleep(time.Second)
			log.Printf("added %v keys to subscribers", addedKeys)
			addedKeys = 0
		}
	}()
	if url != "" {
		return kg.gatherClavisKeys(url)
	} else {
		return kg.gatherRandomKeys(16, 16)
	}
}

func (kg *KeyGatherer) distributeKey(keyId, keyVal []byte) error {
	fmt.Printf("\tk: %v\r", utils.BytesToHexOctets(keyVal))
	var wg sync.WaitGroup
	var result error
	for _, manager := range kg.subscribers {
		wg.Add(1)
		go func(manager *KeyManager) {
			//log.Printf("adding key %v to %v", utils.BytesToHexOctets(keyId), manager.L)
			err := manager.addKey(keyId, keyVal)
			if err != nil {
				result = err
			}
			//log.Printf("added key %v to %v", utils.BytesToHexOctets(keyId), manager.L)
			wg.Done()
		}(manager)
	}
	wg.Wait()
	addedKeys += 1
	return result
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

		err = kg.distributeKey([]byte(keyId), keyVal)
		if err != nil {
			return err
		}
	}
	return nil
}

func (kg *KeyGatherer) gatherRandomKeys(keyIdLength, keyValLength int) error {
	for {
		keyId, keyVal := make([]byte, keyIdLength), make([]byte, keyValLength)
		if _, err := rand.Read(keyId); err != nil {
			return err
		}
		if _, err := rand.Read(keyVal); err != nil {
			return err
		}
		err := kg.distributeKey(keyId, keyVal)
		if err != nil {
			return err
		}
		//time.Sleep(time.Duration(mathRand.Float32() * 5000000000))
	}
}
