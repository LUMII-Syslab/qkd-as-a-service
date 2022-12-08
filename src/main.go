package main

import (
	"log"
)

func main() {
	config := loadConfig()
	keys := initKeyManager(config.MaxKeyCount, config.Aija)

	if config.ClavisURL != "" {
		go gatherClavisKeys(keys, config.ClavisURL)
	} else {
		log.Println("clavis url is empty. generating pseudo random keys")
		go gatherRandomKeys(keys)
	}

	listenAndServe(keys)
}
