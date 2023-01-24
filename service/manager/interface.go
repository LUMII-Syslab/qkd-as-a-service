package manager

func NewKeyManager(maxKeyCount int, aija bool) *KeyManager {
	return newKeyManager(maxKeyCount, aija)
}

func (k *KeyManager) AddKey(keyId []byte, keyVal []byte) error {
	return k.addKey(keyId, keyVal)
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
