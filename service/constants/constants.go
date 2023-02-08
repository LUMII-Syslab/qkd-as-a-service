package constants

const (
	Empty     = 0
	Receiving = 1
	Running   = 2
	DefMxReq  = 1000

	ReserveKeyAndGetHalfRequest = 0x01
	RserveKeyAndGetHalfResponse = 0xff
	GetKeyHalfRequest           = 0x02
	GetKeyHalfResponse          = 0xfe
	GetStateRequest             = 0x03
	GetStateResponse            = 0xfd
	SetStateRequest             = 0x04
	SetStateResponse            = 0xfc

	NoError          = 0
	ErrorKeyNotFound = 1
	ErrorNotRunning  = 2
	ErrorInternal    = 3
	ErrorInvalidReq  = 4
)
