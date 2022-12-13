package utils

func IntToBytes(x int) []byte {
	res := make([]byte, 0)
	mask := (1 << 8) - 1
	for i := 3; i >= 0; i-- {
		shiftedMask := mask << (8 * i)
		maskedValue := x & shiftedMask
		if maskedValue != 0 || shiftedMask < x {
			maskedValue >>= 8 * i
			res = append(res, byte(maskedValue))
		}
	}
	return res
}

func BytesToInt(b []byte) int {
	res := 0
	for _, v := range b {
		res <<= 8
		res += int(v)
	}
	return res
}