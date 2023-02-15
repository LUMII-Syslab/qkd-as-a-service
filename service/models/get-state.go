package models

type GetStateRequest struct {
}

type GetStateResponse struct {
	ErrId      int
	State      int
	KeysStored uint64
	Reservable uint64
	KeysServed uint64
	KeysAdded  uint64
	KeyId0     []byte
	KeyId1     []byte
}
