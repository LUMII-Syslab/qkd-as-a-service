package main

import (
	"log"
	"qkdc-service/api"
	"qkdc-service/data"
	"qkdc-service/gatherers"
	"time"
)

func main() {
	config := loadConfig()

	var gatherer gatherers.KeyGatherer
	switch config.Gatherer {
	case "pseudorandom":
		gatherer = gatherers.NewRandomKeyGatherer(32, 32)
	case "clavis":
		gatherer = gatherers.NewClavisKeyGatherer(config.ClavisURL)
	case "filesystem":
		gatherer = gatherers.NewFileSystemKeyGatherer(config.FSGathererDir)
	}

	if config.AijaEnabled {
		aijaKeyManager := data.NewKeyManager(config.MaxKeyCount, true)
		gatherer.PublishTo(aijaKeyManager)
		go api.ListenAndServe(aijaKeyManager, config.AijaAPIPort, config.LogRequests)
	}

	if config.BrencisEnabled {
		brencisKeyManager := data.NewKeyManager(config.MaxKeyCount, false)
		gatherer.PublishTo(brencisKeyManager)
		go api.ListenAndServe(brencisKeyManager, config.BrencisAPiPort, config.LogRequests)
	}

	time.Sleep(time.Millisecond * 100)

	log.Fatal(gatherer.Start())
}
