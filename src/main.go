package main

import (
	"log"
	"qkdc-service/src/api"
	"qkdc-service/src/data"
)

func main() {
	config := loadConfig()
	keys := data.InitKeyManager(config.MaxKeyCount, config.Aija)

	if config.ClavisURL != "" {
		go data.GatherClavisKeys(keys, config.ClavisURL)
	} else {
		log.Println("clavis url is empty. generating pseudo random keys")
		go data.GatherRandomKeys(keys)
	}

	api.ListenAndServe(keys, config.APIPort)
}
