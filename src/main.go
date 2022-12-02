package main

import (
	"fmt"
	"log"
)

func main() {
	config := loadConfig()
	keys := initKeyManager(config)

	if config.ClavisURL != "" {
		gatherClavisKeys(keys, config.ClavisURL)
	} else {
		log.Println("clavis url is empty. generating pseudo random keys")
		gatherRandomKeys(keys)
	}

	for key, val := range keys.getAll() {
		fmt.Println(key, val)
	}
}
