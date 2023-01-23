package api

import (
	"fmt"
	"qkdc-service/utils"
)

const (
	SeqId = 0x30
	IntId = 0x02
	ArrId = 0x04
	ObjId = 0x06
)

type SequenceElement struct {
	id    byte
	value []byte
}

func (e SequenceElement) AsInt() int {
	return utils.BytesToInt(e.value)
}

func (e SequenceElement) AsBytes() []byte {
	return e.value
}

func (e SequenceElement) encode() []byte {
	res := make([]byte, 2, 2+len(e.value))
	res[0] = e.id
	res[1] = byte(len(e.value))
	res = append(res, e.value...)
	return res
}

func CreateIntSeqElement(x int) SequenceElement {
	res := SequenceElement{}
	res.id = IntId
	res.value = utils.IntToBytes(int(x))
	return res
}

func CreateObjSeqElement(o []byte) SequenceElement {
	res := SequenceElement{}
	res.id = ObjId
	res.value = o
	return res
}

func CreateArrSeqElement(b []byte) SequenceElement {
	res := SequenceElement{}
	res.id = ArrId
	res.value = b
	return res
}

type DERSequence []SequenceElement

func (s DERSequence) ToByteArray() []byte {
	res := make([]byte, 2)
	res[0] = SeqId
	defer func() { res[1] = byte(len(res) - 2) }()

	for _, v := range s {
		res = append(res, v.encode()...)
	}

	return res
}

func (s DERSequence) ToString() string {
	encoded := s.ToByteArray()
	res := ""
	for i, v := range encoded {
		if i > 0 {
			res += " "
		}
		res += fmt.Sprintf("%02X", v)
	}
	return res
}

func DecodeDERSequence(arr []byte) (DERSequence, error) {
	if arr[0] != SeqId {
		return nil, fmt.Errorf("first element should be sequence identifier")
	}
	length := uint(arr[1])
	if 2+length > uint(len(arr)) {
		return nil, fmt.Errorf("not enough bytes supplied")
	}
	res := DERSequence{}
	for i := uint(2); i < 2+length; {
		if arr[i] != IntId && arr[i] != ArrId && arr[i] != ObjId {
			return nil, fmt.Errorf("expected identifier, received %v at pos %v", arr[i], i)
		}
		elem := SequenceElement{arr[i], arr[i+2 : i+2+uint(arr[i+1])]}
		res = append(res, elem)
		i = i + 2 + uint(arr[i+1])
	}
	return res, nil
}
