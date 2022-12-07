package main

import (
	"errors"
	"fmt"
)

type KeyManager struct {
	data        map[string]string
	queue       chan string
	maxKeyCount int
}

func (k *KeyManager) add(id, val string) error {
	if _, ok := k.data[id]; ok {
		return errors.New("key already exists")
	}
	k.data[id] = val
	k.queue <- id
	return nil
}

// returns key id and marks it as reserved
func (k *KeyManager) reserveKey() string {
	key := <-k.queue
	return key
}

func (k *KeyManager) getKeyValue(id string) (string, error) {
	val, ok := k.data[id]
	if !ok {
		return "", errors.New(fmt.Sprintf("key %v not found in data", id))
	}
	return val, nil
}

func (k *KeyManager) getKeyLeft(id string) (string, error) {
	res, err := k.getKeyValue(id)
	if err != nil {
		return "", err
	}
	return res[:len(res)/2+1], nil
}

func (k *KeyManager) getKeyRight(id string) (string, error) {
	res, err := k.getKeyValue(id)
	if err != nil {
		return "", err
	}
	return res[len(res)/2+1:], nil
}

func (k *KeyManager) getAllKeys() map[string]string {
	return k.data
}

func initKeyManager(maxKeyCount int) KeyManager {
	return KeyManager{make(map[string]string), make(chan string, maxKeyCount), maxKeyCount}
}
