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
	param = 256
	want = []byte{0b1, 0b00000000}
	result = IntToBytes(param)
	if !reflect.DeepEqual(want, result) {
		t.Fatalf(`IntToBytes(%v) = %v, wanted %v`, param, result, want)
	}
	param = 1 << 16
	want = []byte{0b1, 0b00000000, 0b00000000}
	result = IntToBytes(param)
	if !reflect.DeepEqual(want, result) {
		t.Fatalf(`IntToBytes(%v) = %v, wanted %v`, param, result, want)
	}
}

func TestBytesToInt(t *testing.T) {
	param := []byte{0b111}
	want := 7
	result := BytesToInt(param)
	if !reflect.DeepEqual(want, result) {
		t.Fatalf(`BytesToInt(%v) = %v, wanted %v`, param, result, want)
	}
	param = []byte{0b1001, 0b00101000}
	want = 2344
	result = BytesToInt(param)
	if !reflect.DeepEqual(want, result) {
		t.Fatalf(`BytesToInt(%v) = %v, wanted %v`, param, result, want)
	}
}
