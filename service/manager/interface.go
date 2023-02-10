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

func (k *KeyManager) GetKeyHalf(request *models.GKHRequest) (response *models.GKHResponse) {
	response = new(models.GKHResponse)
	response.ThisHalf, response.OtherHash, response.ErrId = k.GetKeyThisHalfOtherHash(request.KeyId)
	response.HashAlgId = k.HashAlgId
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

func (k *KeyManager) GetState(_ *models.GetStateRequest) (response *models.GetStateResponse) {
	response = new(models.GetStateResponse)
	keyManagerState := k.getManagerState()
	if keyManagerState.ReservableSize == 0 {
		response.State = constants.Empty
	} else if keyManagerState.Running == false {
		response.State = constants.Receiving
	} else {
		response.State = constants.Running
	}
	response.KeyId0 = keyManagerState.OldestEvenKey.KeyId
	response.KeyId1 = keyManagerState.OldestOddKey.KeyId
	return
}

func (k *KeyManager) GetFullState() KeyManagerState {
	return k.getManagerState()
}
