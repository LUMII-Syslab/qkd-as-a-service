package gatherers

import (
	"crypto/rand"
	"log"
	"time"
)

type RandomKeyGatherer struct {
	keyGathererBase
	keyIdLength  int
	keyValLength int
}

func NewRandomKeyGatherer(keyIdLength, keyValLength int) *RandomKeyGatherer {
	return &RandomKeyGatherer{keyGathererBase{make([]KeyGathererListener, 0), 0}, keyIdLength, keyValLength}
}

func (kg *RandomKeyGatherer) Start() error {
	genTicker := time.NewTicker(time.Millisecond)
	for {
		<-genTicker.C
		keyId, keyVal := make([]byte, kg.keyIdLength), make([]byte, kg.keyValLength)
		if _, err := rand.Read(keyId); err != nil {
			return err
		}
		if _, err := rand.Read(keyVal); err != nil {
			return err
		}
		err := kg.distributeKey(keyId, keyVal)
		if err != nil {
			log.Println(err)
		}
	}
}
