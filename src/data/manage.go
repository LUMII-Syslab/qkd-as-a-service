package data

import (
	"errors"
	"fmt"
	"log"
)

type KeyManager struct {
	data        map[string][]byte
	reserved    map[string]bool
	queue       chan []byte
	maxKeyCount int
	aija        bool // true <-> returns left of key ( otherwise returns right )
}

func (k *KeyManager) add(id, val []byte) error {
	if _, ok := k.data[string(id)]; ok {
		return errors.New("key already exists")
	}
	k.data[string(id)] = val
	k.queue <- id
	return nil
}

// returns key id and marks it as reserved
func (k *KeyManager) ReserveKey() []byte {
	key := <-k.queue
	k.reserved[string(key)] = true
	return key
}

func (k *KeyManager) ReserveKeyAndGetHalf(length uint) (keyId []byte, thisHalf []byte, otherHash []byte) {
	keyId = k.ReserveKey()
	var err error
	if k.aija {
		thisHalf, err = k.GetKeyLeft(keyId)
		otherHash, err = k.GetKeyRightHash(keyId)
	} else {
		thisHalf, err = k.GetKeyRight(keyId)
		otherHash, err = k.GetKeyLeftHash(keyId)
	}
	if err != nil {
		log.Panic(err)
	}
	return
}

func (k *KeyManager) GetKeyValue(id []byte) ([]byte, error) {
	val, ok := k.data[string(id)]
	if !ok {
		return nil, errors.New(fmt.Sprintf("key %v not found in data", id))
	}
	return val, nil
}

func (k *KeyManager) GetKeyLeft(id []byte) ([]byte, error) {
	res, err := k.GetKeyValue(id)
	if err != nil {
		return nil, err
	}
	return res[:len(res)/2+1], nil
}

func (k *KeyManager) GetKeyLeftHash(id []byte) ([]byte, error) {
	return k.GetKeyLeft(id) // TODO fix this
}

func (k *KeyManager) GetKeyRight(id []byte) ([]byte, error) {
	res, err := k.GetKeyValue(id)
	if err != nil {
		return nil, err
	}
	return res[len(res)/2+1:], nil
}

func (k *KeyManager) GetKeyRightHash(id []byte) ([]byte, error) {
	return k.GetKeyRight(id) // TODO fix this
}

func (k *KeyManager) getAllKeys() map[string][]byte {
	return k.data
}

func InitKeyManager(maxKeyCount int, aija bool) KeyManager {
	return KeyManager{make(map[string][]byte), make(map[string]bool), make(chan []byte, maxKeyCount), maxKeyCount, aija}
}
