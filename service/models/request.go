package models

type Request struct {
	RequestId int
	CNonce    int
	Sequence  *DERSequence
}
