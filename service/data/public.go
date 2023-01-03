package data

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
