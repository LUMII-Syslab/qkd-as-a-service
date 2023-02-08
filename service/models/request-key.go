package models

type RKAGHRequest struct {
	KeyLength int
}

type RKAGHResponse struct {
	ErrCode   int
	KeyId     []byte
	ThisHalf  []byte
	OtherHash []byte
	HashAlgId []byte
}
