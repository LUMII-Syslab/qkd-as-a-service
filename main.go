package main

import "fmt"

func main() {
	config := LoadConfig()
	keys := InitKeyManager(config)

	gatherKeys(keys, config)

	for key, val := range keys.getAll() {
		fmt.Println(key, val)
	}
}
