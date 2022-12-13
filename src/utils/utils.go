package utils

func IntToBytes(x int) []byte {
	res := make([]byte, 0)
	mask := (1 << 8) - 1
	for i := 3; i >= 0; i-- {
		shiftedMask := mask << (8 * i)
		maskedValue := x & shiftedMask
		if maskedValue != 0 {
			maskedValue >>= 8 * i
			res = append(res, byte(maskedValue))
		}
	}
	return res
}
