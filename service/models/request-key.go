package models

type RKAGHRequest struct {
	KeyLength int
}

type RKAGHResponse struct {
	ErrId     int
	KeyId     []byte
	ThisHalf  []byte
	OtherHash []byte
	HashAlgId []byte
}
