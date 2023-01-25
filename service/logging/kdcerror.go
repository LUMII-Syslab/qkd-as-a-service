package logging

type KDCError struct {
	Code  int
	Error error
}

func NewKDCError(code int, err error) *KDCError {
	return &KDCError{code, err}
}

func (k *KDCError) ToString() string {
	return k.Error.Error()
}
