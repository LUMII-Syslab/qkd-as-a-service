package models

type SetStateRequest struct {
	State  int
	KeyId0 []byte
	KeyId1 []byte
}

type SetStateResponse struct {
	ErrId int
}
