package main

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
func (k *KeyManager) reserveKey() []byte {
	key := <-k.queue
	k.reserved[string(key)] = true
	return key
}

func (k *KeyManager) reserveKeyAndGetHalf(length uint) (keyId []byte, thisHalf []byte, otherHash []byte) {
	keyId = k.reserveKey()
	var err error
	if k.aija {
		thisHalf, err = k.getKeyLeft(keyId)
		otherHash, err = k.getKeyRightHash(keyId)
	} else {
		thisHalf, err = k.getKeyRight(keyId)
		otherHash, err = k.getKeyLeftHash(keyId)
	}
	if err != nil {
		log.Panic(err)
	}
	return
}

func (k *KeyManager) getKeyValue(id []byte) ([]byte, error) {
	val, ok := k.data[string(id)]
	if !ok {
		return nil, errors.New(fmt.Sprintf("key %v not found in data", id))
	}
	return val, nil
}

func (k *KeyManager) getKeyLeft(id []byte) ([]byte, error) {
	res, err := k.getKeyValue(id)
	if err != nil {
		return nil, err
	}
	return res[:len(res)/2+1], nil
}

func (k *KeyManager) getKeyLeftHash(id []byte) ([]byte, error) {
	return k.getKeyLeft(id) // TODO fix this
}

func (k *KeyManager) getKeyRight(id []byte) ([]byte, error) {
	res, err := k.getKeyValue(id)
	if err != nil {
		return nil, err
	}
	return res[len(res)/2+1:], nil
}

func (k *KeyManager) getKeyRightHash(id []byte) ([]byte, error) {
	return k.getKeyRight(id) // TODO fix this
}

func (k *KeyManager) getAllKeys() map[string][]byte {
	return k.data
}

func initKeyManager(maxKeyCount int, aija bool) KeyManager {
	return KeyManager{make(map[string][]byte), make(map[string]bool), make(chan []byte, maxKeyCount), maxKeyCount, aija}
}
