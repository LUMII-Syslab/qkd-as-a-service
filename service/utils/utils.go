package utils

import "strconv"

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
	if len(res) == 0 {
		res = append(res, 0)
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

func BytesToHexOctets(b []byte) string {
	res := ""
	for i, v := range b {
		if i > 0 {
			res += " "
		}
		tmp := strconv.FormatInt(int64(v), 16)
		for len(tmp) < 2 {
			tmp = "0" + tmp
		}
		res += tmp
	}
	return res
}

func ByteSum(b []byte) int {
	sum := 0
	for _, v := range b {
		sum += int(v)
	}
	return sum
}
