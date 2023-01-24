package manager

import (
	"golang.org/x/crypto/sha3"
)

func (k *KeyManager) getThisHalf(keyId []byte) ([]byte, error) {
	if k.servesLeft {
		return k.getKeyLeft(keyId)
	} else {
		return k.getKeyRight(keyId)
	}
}

func (k *KeyManager) getOtherHash(keyId []byte) ([]byte, error) {
	if k.servesLeft {
		return k.getKeyRightHash(keyId)
	} else {
		return k.getKeyLeftHash(keyId)
	}
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

func (k *KeyManager) getKeyLeft(id []byte) ([]byte, error) {
	res, err := k.getKey(id)
	if err != nil {
		return nil, err
	}
	return res.KeyVal[:len(res.KeyVal)/2+1], nil
}

func (k *KeyManager) getKeyLeftHash(id []byte) ([]byte, error) {
	data, err := k.getKeyLeft(id)
	if err != nil {
		return nil, err
	}
	return k.getShake128Hash(data)
}

func (k *KeyManager) getKeyRight(id []byte) ([]byte, error) {
	res, err := k.getKey(id)
	if err != nil {
		return nil, err
	}
	return res.KeyVal[len(res.KeyVal)/2+1:], nil
}

func (k *KeyManager) getKeyRightHash(id []byte) ([]byte, error) {
	data, err := k.getKeyRight(id)
	if err != nil {
		return nil, err
	}
	return k.getShake128Hash(data)
}
