package models

type GetStateRequest struct {
}

type GetStateResponse struct {
	ErrId  int
	State  int
	KeyId0 []byte
	KeyId1 []byte
}
