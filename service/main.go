package main

import (
	"log"
	"os"
	"qkdc-service/api"
	"qkdc-service/gatherers"
	"qkdc-service/manager"
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
		aijaInfoLogger := log.New(os.Stdout, "AIJA INFO: ", log.Ldate|log.Ltime)
		aijaErrorLogger := log.New(os.Stdout, "AIJA ERROR: ", log.Ldate|log.Ltime|log.Lshortfile)

		aijaKeyManager := manager.NewKeyManager(config.MaxKeyCount, true)
		gatherer.PublishTo(aijaKeyManager)
		go api.ListenAndServe(aijaKeyManager, aijaInfoLogger, aijaErrorLogger, config.AijaAPIPort)
	}

	if config.BrencisEnabled {
		brencisInfoLogger := log.New(os.Stdout, "BRENCIS INFO: ", log.Ldate|log.Ltime)
		brencisErrorLogger := log.New(os.Stdout, "BRENCIS ERROR: ", log.Ldate|log.Ltime|log.Lshortfile)

		brencisKeyManager := manager.NewKeyManager(config.MaxKeyCount, false)
		gatherer.PublishTo(brencisKeyManager)
		go api.ListenAndServe(brencisKeyManager, brencisInfoLogger, brencisErrorLogger, config.BrencisAPiPort)
	}

	time.Sleep(time.Millisecond * 100)

	log.Fatal(gatherer.Start())
}
