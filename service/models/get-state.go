package models

type GetStateRequest struct {
}

type GetStateResponse struct {
	ErrId      int
	State      int
	KeysStored int
	Reservable int
	KeysServed int
	KeysAdded  int
	KeyId0     []byte
	KeyId1     []byte
}
