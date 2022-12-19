package data

import (
	"errors"
	"fmt"
	"golang.org/x/crypto/sha3"
	"log"
	"qkdc-service/utils"
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

func (k *KeyManager) GetKeyThisHalfOtherHash(keyId []byte) (thisHalf []byte, otherHash []byte, err error) {
	thisHalf, err = k.getThisHalf(keyId)
	if err != nil {
		return
	}
	otherHash, err = k.getOtherHash(keyId)
	return
}

func (k *KeyManager) ReserveKeyAndGetHalf() (keyId []byte, thisHalf []byte, otherHash []byte, err error) {
	keyId = k.ReserveKey()
	thisHalf, otherHash, err = k.GetKeyThisHalfOtherHash(keyId)
	return
}

func (k *KeyManager) getShake128Hash(data []byte) (hash []byte, err error) {
	h := sha3.NewShake128()
	hash = make([]byte, 128)
	_, err = h.Write(data)
	if err != nil {
		return
	}
	_, err = h.Read(hash)
	return
}

func (k *KeyManager) GetKeyLeft(id []byte) ([]byte, error) {
	res, err := k.getKeyValue(id)
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
	return k.getShake128Hash(data)
}

func (k *KeyManager) GetKeyRight(id []byte) ([]byte, error) {
	res, err := k.getKeyValue(id)
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
	return k.getShake128Hash(data)
}
