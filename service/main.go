package main

import (
	"log"
	"qkdc-service/api"
	"qkdc-service/data"
	"qkdc-service/gatherer"
	"time"
)

func main() {
	config := loadConfig()

	pseudoRandGatherer := gatherer.NewRandomKeyGatherer()

	if config.AijaEnabled {
		aijaKeyManager := data.NewKeyManager(config.MaxKeyCount, true)
		pseudoRandGatherer.PublishTo(aijaKeyManager)
		go api.ListenAndServe(aijaKeyManager, config.AijaAPIPort, config.LogRequests)
	}

	if config.BrencisEnabled {
		brencisKeyManager := data.NewKeyManager(config.MaxKeyCount, false)
		pseudoRandGatherer.PublishTo(brencisKeyManager)
		go api.ListenAndServe(brencisKeyManager, config.BrencisAPiPort, config.LogRequests)
	}

	time.Sleep(time.Millisecond * 100)

	log.Fatal(pseudoRandGatherer.Start())
}
