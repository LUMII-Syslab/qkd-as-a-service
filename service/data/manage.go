package data

import (
	"errors"
	"fmt"
	"log"
	"qkdc-service/utils"
)

type Key struct {
	KeyId  []byte
	KeyVal []byte
}

type KeyManager struct {
	A *SyncDeque[Key]       // all keys
	B *SyncDeque[Key]       // reservable keys
	C *SyncDeque[Key]       // queue into B
	D *SyncMap[string, Key] // key id, val dictionary
	W int                   // maximum A size
	L bool                  // true <-> returns left of key and serves even ( otherwise returns right and serves odd)
}

func InitKeyManager(maxKeyCount int, aija bool) *KeyManager {
	return &KeyManager{
		A: NewSyncDeque[Key](),
		B: NewSyncDeque[Key](),
		C: NewSyncDeque[Key](),
		D: NewSyncMap[string, Key](),
		W: maxKeyCount,
		L: aija,
	}
}

func (k *KeyManager) keyExists(id []byte) bool {
	k.dataMu.Lock()
	_, ok := k.data[string(id)]
	k.dataMu.Unlock()
	return ok
}

func (k *KeyManager) getKeyValue(id []byte) ([]byte, error) {
	k.dataMu.Lock()
	val, ok := k.data[string(id)]
	k.dataMu.Unlock()
	if !ok {
		return nil, errors.New(fmt.Sprintf("key %v not found in data", id))
	}
	return val, nil
}

func (k *KeyManager) addKey(id, val []byte) error {
	if k.keyExists(id) {
		return errors.New(fmt.Sprintf("key %v already exists", utils.BytesToHexOctets(id)))
	}
	// it is important to save the key and then add it to queue
	k.dataMu.Lock()
	k.data[string(id)] = val
	k.dataMu.Unlock()
	sum := 0
	for _, v := range val {
		sum += int(v)
	}
	if k.aija == (sum%2 == 0) {
		k.queue <- id
	}
	return nil
}

// ReserveKey returns key id and marks it as reserved
func (k *KeyManager) ReserveKey() []byte {
	log.Println(len(k.queue))
	key := <-k.queue
	log.Println(len(k.queue), utils.BytesToHexOctets(key))
	return key
}
