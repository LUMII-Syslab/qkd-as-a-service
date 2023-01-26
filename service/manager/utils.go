package manager

import (
	"golang.org/x/crypto/sha3"
	"qkdc-service/constants"
	"qkdc-service/logging"
)

func (k *KeyManager) getThisHalf(keyId []byte) ([]byte, *logging.KDCError) {
	if k.aija {
		return k.getKeyLeft(keyId)
	} else {
		return k.getKeyRight(keyId)
	}
}

func (k *KeyManager) getOtherHash(keyId []byte) ([]byte, *logging.KDCError) {
	if k.aija {
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

func (k *KeyManager) getKeyLeft(id []byte) ([]byte, *logging.KDCError) {
	res, err := k.getKey(id)
	if err != nil {
		return nil, err
	}
	return res.KeyVal[:len(res.KeyVal)/2+1], nil
}

func (k *KeyManager) getKeyLeftHash(id []byte) ([]byte, *logging.KDCError) {
	data, kdcErr := k.getKeyLeft(id)
	if kdcErr != nil {
		return nil, kdcErr
	}
	hash, err := k.getShake128Hash(data)
	if err != nil {
		return nil, logging.NewKDCError(constants.ErrorInternal, err)
	}
	return hash, nil
}

func (k *KeyManager) getKeyRight(id []byte) ([]byte, *logging.KDCError) {
	res, err := k.getKey(id)
	if err != nil {
		return nil, err
	}
	return res.KeyVal[len(res.KeyVal)/2+1:], nil
}

func (k *KeyManager) getKeyRightHash(id []byte) ([]byte, *logging.KDCError) {
	data, kdcErr := k.getKeyRight(id)
	if kdcErr != nil {
		return nil, kdcErr
	}
	hash, err := k.getShake128Hash(data)
	if err != nil {
		return nil, logging.NewKDCError(constants.ErrorInternal, err)
	}
	return hash, nil
}
