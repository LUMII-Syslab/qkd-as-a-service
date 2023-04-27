package encoding

import "qkdc-service/utils"

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

func (e SequenceElement) ToByteArray() []byte {
	res := make([]byte, 2, 2+len(e.value))
	res[0] = e.id
	res[1] = byte(len(e.value))
	res = append(res, e.value...)
	return res
}

func CreateIntSeqElement[T int | int64 | uint64](x T) SequenceElement {
	res := SequenceElement{}
	res.id = IntId

	res.value = utils.IntToBytes(int64(x))
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
