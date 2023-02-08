package manager

import (
	"log"
	"qkdc-service/constants"
	"qkdc-service/models"
)

func NewKeyManager(maxKeyCount uint64, aija bool, logger *log.Logger) *KeyManager {
	return newKeyManager(maxKeyCount, aija, logger)
}

func (k *KeyManager) AddKey(keyId []byte, keyVal []byte) error {
	return k.addKey(keyId, keyVal)
}

func (k *KeyManager) GetKeyThisHalfOtherHash(keyId []byte) (thisHalf []byte, otherHash []byte, errId int) {
	thisHalf, errId = k.getThisHalf(keyId)
	if errId != constants.NoError {
		return
	}
	otherHash, errId = k.getOtherHash(keyId)
	return
}

func (k *KeyManager) ReserveKeyAndGetHalf(_ *models.RKAGHRequest) (response *models.RKAGHResponse) {
	response = new(models.RKAGHResponse)

	key, errId := k.extractKey()

	response.ErrId = errId
	if response.ErrId != constants.NoError {
		return
	}

	response.KeyId = key.KeyId
	response.ThisHalf, response.OtherHash, response.ErrId = k.GetKeyThisHalfOtherHash(response.KeyId)
	response.HashAlgId = k.HashAlgId

	return
}

func (k *KeyManager) GetState() (state int, keyId0 []byte, keyId1 []byte, errId int) {
	keyManagerState := k.getManagerState()
	if keyManagerState.ReservableSize == 0 {
		state = constants.Empty
	} else if keyManagerState.Running == false {
		state = constants.Receiving
	} else {
		state = constants.Running
	}
	return
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
