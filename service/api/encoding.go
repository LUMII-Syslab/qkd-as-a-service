package api

import (
	"fmt"
	"math"
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

func CreateIntSeqElement(x interface{}) SequenceElement {
	res := SequenceElement{}
	res.id = IntId
	switch x.(type) {
	case int:
		res.value = utils.IntToBytes(int64(x.(int)))
	case int64:
		res.value = utils.IntToBytes(x.(int64))
	case uint64:
		res.value = utils.IntToBytes(int64(x.(uint64)))
	default:
		panic("unsupported type")
	}
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
	sequence := make([]byte, 0)
	for _, v := range s {
		sequence = append(sequence, v.encode()...)
	}
	res := make([]byte, 2, 2+len(sequence))
	res[0] = SeqId

	length := len(sequence)
	if length > 127 {
		lengthOfLength := int(math.Ceil(math.Log2(float64(length))) / 8) // in bytes
		// fmt.Printf("Length of %d is %d", length, lengthOfLength)
		res[1] = 0b10000000 | byte(lengthOfLength)
		res = append(res, utils.IntToBytesWithLength(int64(length), lengthOfLength)...)
	} else {
		res[1] = byte(length)
	}

	res = append(res, sequence...)
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

func DecodeIntoVariables(sequence DERSequence, vars ...interface{}) error {
	if len(sequence) != len(vars) {
		return fmt.Errorf("expected %v elements, received %v", len(vars), len(sequence))
	}
	for i, v := range vars {
		switch v.(type) {
		case *int:
			*v.(*int) = sequence[i].AsInt()
		case *[]byte:
			*v.(*[]byte) = sequence[i].AsBytes()
		case nil:
			continue
		default:
			return fmt.Errorf("unsupported type")
		}
	}
	return nil
}
