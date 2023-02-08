package manager

import (
	"golang.org/x/crypto/sha3"
	"qkdc-service/constants"
)

func (k *KeyManager) getThisHalf(keyId []byte) (thisHalf []byte, errId int) {
	if k.aija {
		return k.getKeyLeft(keyId)
	} else {
		return k.getKeyRight(keyId)
	}
}

func (k *KeyManager) getOtherHash(keyId []byte) ([]byte, int) {
	if k.aija {
		return k.getKeyRightHash(keyId)
	} else {
		return k.getKeyLeftHash(keyId)
	}
}

func (k *KeyManager) getShake128Hash(data []byte) (hash []byte, errId int) {
	h := sha3.NewShake128()
	hash = make([]byte, 0)
	_, err := h.Write(data)
	if err != nil {
		errId = constants.ErrorInternal
		return
	}
	_, err = h.Read(hash)
	if err != nil {
		errId = constants.ErrorInternal
		return
	}
	hash = []byte{1, 2}
	return
}

func (k *KeyManager) getKeyLeft(id []byte) (keyLeft []byte, errId int) {
	var key *Key
	key, errId = k.getKey(id)
	if errId != constants.NoError {
		return
	}
	keyLeft = key.KeyVal[:(len(key.KeyVal)+1)/2]
	return
}

func (k *KeyManager) getKeyLeftHash(id []byte) (leftHash []byte, errId int) {
	data, errId := k.getKeyLeft(id)
	if errId != constants.NoError {
		return
	}
	leftHash, errId = k.getShake128Hash(data)
	if errId != constants.NoError {
		return
	}
	return
}

func (k *KeyManager) getKeyRight(id []byte) (keyRight []byte, errId int) {
	var key *Key
	key, errId = k.getKey(id)
	if errId != constants.NoError {
		return
	}
	keyRight = key.KeyVal[(len(key.KeyVal)+1)/2:]
	return
}

func (k *KeyManager) getKeyRightHash(id []byte) (rightHash []byte, errId int) {
	data, errId := k.getKeyRight(id)
	if errId != constants.NoError {
		return
	}
	rightHash, errId = k.getShake128Hash(data)
	if errId != constants.NoError {
		return
	}
	return
}
