package gatherer

import (
	"crypto/rand"
	"time"
)

type RandomKeyGatherer struct {
	keyGathererBase
	keyIdLength  int
	keyValLength int
}

func (kg *RandomKeyGatherer) Start() error {
	for {
		keyId, keyVal := make([]byte, kg.keyIdLength), make([]byte, kg.keyValLength)
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
		time.Sleep(time.Millisecond)
	}
}
