package manager

import (
	"log"
	"qkdc-service/constants"
	"qkdc-service/logging"
)

func NewKeyManager(maxKeyCount uint64, aija bool, logger *log.Logger) *KeyManager {
	return newKeyManager(maxKeyCount, aija, logger)
}

func (k *KeyManager) AddKey(keyId []byte, keyVal []byte) error {
	return k.addKey(keyId, keyVal)
}

func (k *KeyManager) GetKeyThisHalfOtherHash(keyId []byte) (thisHalf []byte, otherHash []byte, err *logging.KDCError) {
	thisHalf, err = k.getThisHalf(keyId)
	if err != nil {
		return
	}
	otherHash, err = k.getOtherHash(keyId)
	return
}

func (k *KeyManager) ReserveKeyAndGetHalf() (keyId []byte, thisHalf []byte, otherHash []byte, err *logging.KDCError) {
	key, err := k.extractKey()
	if err != nil {
		return
	}
	keyId = key.KeyId
	thisHalf, otherHash, err = k.GetKeyThisHalfOtherHash(keyId)
	return
}

func (k *KeyManager) GetState() int {
	state := k.getManagerState()
	if state.ReservableSize == 0 {
		return constants.Empty
	} else if state.Running == false {
		return constants.Receiving
	} else {
		return constants.Running
	}
}

func (k *KeyManager) GetFullState() KeyManagerState {
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
