package models

type GKHRequest struct {
	KeyLength int
	KeyId     []byte
}

type GKHResponse struct {
	ErrId     int
	ThisHalf  []byte
	OtherHash []byte
	HashAlgId []byte
}
