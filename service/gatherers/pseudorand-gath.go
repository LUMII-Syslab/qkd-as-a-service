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
	limitSpeed   bool
}

func NewPseudorandomKeyGatherer(keyIdLength, keyValLength int, limitSpeed bool) *RandomKeyGatherer {
	return &RandomKeyGatherer{keyGathererBase{make([]KeyGathererListener, 0), 0}, keyIdLength, keyValLength, limitSpeed}
}

func (kg *RandomKeyGatherer) Start() error {
	genTicker := time.NewTicker(time.Millisecond)
	for {
		if kg.limitSpeed {
			<-genTicker.C
		}
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
