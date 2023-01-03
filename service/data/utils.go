package data

import "golang.org/x/crypto/sha3"

func (k *KeyManager) getThisHalf(keyId []byte) ([]byte, error) {
	if k.L {
		return k.GetKeyLeft(keyId)
	} else {
		return k.GetKeyRight(keyId)
	}
}

func (k *KeyManager) getOtherHash(keyId []byte) ([]byte, error) {
	if k.L {
		return k.GetKeyRightHash(keyId)
	} else {
		return k.GetKeyLeftHash(keyId)
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
