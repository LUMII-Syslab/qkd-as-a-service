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
		t.Fatalf(`IntToBytes(7) = %v, want match for %v`, result, want)
	}
}
