package data

import (
	"errors"
	"fmt"
	"golang.org/x/crypto/sha3"
	"sync"
)

type KeyManager struct {
	data     map[string][]byte
	dataMu   sync.Mutex
	queue    chan []byte
	mxKeyCnt int
	aija     bool // true <-> returns left of key ( otherwise returns right )
}

func InitKeyManager(maxKeyCount int, aija bool) KeyManager {
	return KeyManager{
		data:     make(map[string][]byte),
		queue:    make(chan []byte, maxKeyCount),
		mxKeyCnt: maxKeyCount,
		aija:     aija,
	}
}

func (k *KeyManager) keyExists(id []byte) bool {
	k.dataMu.Lock()
	_, ok := k.data[string(id)]
	k.dataMu.Unlock()
	return ok
}

func (k *KeyManager) AddKey(id, val []byte) error {
	if k.keyExists(id) {
		return errors.New(fmt.Sprintf("key %v already exists", string(id)))
	}
	// it is important to save the key and then add it to queue
	k.dataMu.Lock()
	k.data[string(id)] = val
	k.dataMu.Unlock()
	k.queue <- id
	return nil
}

// ReserveKey returns key id and marks it as reserved
func (k *KeyManager) ReserveKey() []byte {
	key := <-k.queue
	return key
}

func (k *KeyManager) getThisHalf(keyId []byte) ([]byte, error) {
	if k.aija {
		return k.GetKeyLeft(keyId)
	} else {
		return k.GetKeyRight(keyId)
	}
}

func (k *KeyManager) getOtherHash(keyId []byte) ([]byte, error) {
	if k.aija {
		return k.GetKeyRightHash(keyId)
	} else {
		return k.GetKeyLeftHash(keyId)
	}
}

func (k *KeyManager) GetKeyHalf(keyId []byte) (thisHalf []byte, otherHash []byte, err error) {
	thisHalf, err = k.getThisHalf(keyId)
	if err != nil {
		return
	}
	otherHash, err = k.getOtherHash(keyId)
	return
}

func (k *KeyManager) ReserveKeyAndGetHalf() (keyId []byte, thisHalf []byte, otherHash []byte, err error) {
	keyId = k.ReserveKey()
	thisHalf, otherHash, err = k.GetKeyHalf(keyId)
	return
}

func (k *KeyManager) GetKeyValue(id []byte) ([]byte, error) {
	k.dataMu.Lock()
	val, ok := k.data[string(id)]
	k.dataMu.Unlock()
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
	data, err := k.GetKeyLeft(id)
	if err != nil {
		return nil, err
	}
	h := sha3.NewShake128()
	res := make([]byte, 128)
	h.Write(data)
	h.Read(res)
	return res, nil
}

func (k *KeyManager) GetKeyRight(id []byte) ([]byte, error) {
	res, err := k.GetKeyValue(id)
	if err != nil {
		return nil, err
	}
	return res[len(res)/2+1:], nil
}

func (k *KeyManager) GetKeyRightHash(id []byte) ([]byte, error) {
	data, err := k.GetKeyRight(id)
	if err != nil {
		return nil, err
	}
	h := sha3.NewShake128()
	res := make([]byte, 128)
	h.Write(data)
	h.Read(res)
	return res, nil
}
