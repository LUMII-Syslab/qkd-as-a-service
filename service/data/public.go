package data

func InitKeyManager(maxKeyCount int, aija bool) *KeyManager {
	return &KeyManager{
		A: NewSyncDeque[Key](),
		B: NewSyncDeque[Key](),
		C: NewSyncDeque[Key](),
		D: NewSyncMap[string, Key](),
		W: maxKeyCount,
		L: aija,
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
	keyId = k.ReserveKey().KeyVal
	thisHalf, otherHash, err = k.GetKeyThisHalfOtherHash(keyId)
	return
}
