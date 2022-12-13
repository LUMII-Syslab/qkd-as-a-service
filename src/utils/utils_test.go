package utils

import (
	"reflect"
	"testing"
)

func TestIntToBytes(t *testing.T) {
	param := 7
	want := []byte{0b111}
	result := IntToBytes(param)
	if !reflect.DeepEqual(want, result) {
		t.Fatalf(`IntToBytes(%v) = %v, wanted %v`, param, result, want)
	}
	param = 2344
	want = []byte{0b1001, 0b00101000}
	result = IntToBytes(param)
	if !reflect.DeepEqual(want, result) {
		t.Fatalf(`IntToBytes(%v) = %v, wanted %v`, param, result, want)
	}
}
