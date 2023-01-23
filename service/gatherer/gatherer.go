package gatherer

import "sync"

type KeyGathererListener interface {
	AddKey(id []byte, val []byte) error
}

type KeyGatherer interface {
	PublishTo(KeyGathererListener)
	Start() error
	distributeKey(keyId, keyVal []byte) error
}

type keyGathererBase struct {
	subscribers  []KeyGathererListener
	keysGathered int
}

func (kg *keyGathererBase) PublishTo(listener KeyGathererListener) {
	kg.subscribers = append(kg.subscribers, listener)
}

func (kg *keyGathererBase) distributeKey(keyId, keyVal []byte) (err error) {
	var wg sync.WaitGroup
	for _, listener := range kg.subscribers {
		wg.Add(1)
		go func(listener KeyGathererListener) {
			cErr := listener.AddKey(keyId, keyVal)
			if cErr != nil {
				err = cErr
			}
			wg.Done()
		}(listener)
	}
	wg.Wait()
	kg.keysGathered += 1
	return err
}
