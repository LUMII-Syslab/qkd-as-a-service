package main

import (
	"log"
	"qkdc-service/src/api"
	"qkdc-service/src/data"
	"time"
)

func main() {
	config := loadConfig()

	if config.AijaAPIPort == config.BrencisAPiPort {
		log.Fatalln("aija_port and brencis_port should be different")
	}

	gatherer := data.InitKeyGatherer()

	if config.AijaAPIPort != -1 {
		aijaKeys := data.InitKeyManager(config.MaxKeyCount, true)
		gatherer.Subscribe(aijaKeys)
		go api.ListenAndServe(aijaKeys, config.AijaAPIPort)
	}

	if config.BrencisAPiPort != -1 {
		brencisKeys := data.InitKeyManager(config.MaxKeyCount, false)
		gatherer.Subscribe(brencisKeys)
		go api.ListenAndServe(brencisKeys, config.BrencisAPiPort)
	}

	time.Sleep(time.Millisecond * 100)

	err := gatherer.Start(config.ClavisURL)
	if err != nil {
		log.Fatalln(err)
	}
}
