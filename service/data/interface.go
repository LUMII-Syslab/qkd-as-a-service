package data

import (
	"github.com/gammazero/deque"
)

func InitKeyManager(maxKeyCount int, aija bool) *KeyManager {
	return &KeyManager{
		A: deque.New[Key](),
		B: deque.New[Key](),
		C: make(map[string]bool),
		S: make(chan int, maxKeyCount),
		Z: 500,
		D: make(map[string]Key),
		W: maxKeyCount,
		L: aija,
		R: true,
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
	key, err := k.extractKey()
	if err != nil {
		return
	}
	keyId = key.KeyId
	thisHalf, otherHash, err = k.GetKeyThisHalfOtherHash(keyId)
	return
}

func (k *KeyManager) GetState() int {
	return k.getManagerState()
}

func (k *KeyManager) GetFirstKeys() (evenKey []byte, oddKey []byte, err error) {
	evenKey, err = k.getFirstKey(true)
	if err != nil {
		return
	}
	oddKey, err = k.getFirstKey(false)
	return
}
