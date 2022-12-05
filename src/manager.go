package main

import (
	"errors"
)

type KeyManager struct {
	data map[string]string
}

func (k *KeyManager) add(id, val string) error {
	_, ok := k.data[id]
	if ok {
		return errors.New("key already exists")
	}
	k.data[id] = val
	return nil
}

func (k *KeyManager) getVal(id string) (string, error) {
	val, ok := k.data[id]
	if !ok {
		return "", errors.New("key not found in data")
	}
	return val, nil
}

func (k *KeyManager) getAll() map[string]string {
	return k.data
}

func initKeyManager(config Configuration) KeyManager {
	return KeyManager{make(map[string]string)}
}
