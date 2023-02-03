package main

import (
	"io"
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

	var infoEndpoint io.Writer
	infoEndpoint = os.Stdout
	if !config.LogRequests {
		infoEndpoint = io.Discard
	}
	if config.AijaEnabled {
		aijaDebugLogger := log.New(os.Stdout, "AIJA DEBUG\t", log.Ldate|log.Ltime|log.Lshortfile)
		aijaKeyManager := manager.NewKeyManager(config.MaxKeyCount, true, aijaDebugLogger)
		gatherer.PublishTo(aijaKeyManager)

		aijaInfoLogger := log.New(infoEndpoint, "AIJA INFO\t", log.Ldate|log.Ltime)
		aijaErrorLogger := log.New(os.Stdout, "AIJA ERROR\t", log.Ldate|log.Ltime|log.Lshortfile)
		go api.ListenAndServe(aijaKeyManager, aijaInfoLogger, aijaErrorLogger, config.AijaAPIPort)

		go func() {
			for {
				time.Sleep(time.Second * 5)
				state := aijaKeyManager.GetFullState()
				aijaInfoLogger.Printf("state: %+v", state)
			}
		}()
	}

	if config.BrencisEnabled {
		brencisDebugLogger := log.New(io.Discard, "BRENCIS DEBUG\t", log.Ldate|log.Ltime|log.Lshortfile)
		brencisKeyManager := manager.NewKeyManager(config.MaxKeyCount, false, brencisDebugLogger)
		gatherer.PublishTo(brencisKeyManager)

		brencisInfoLogger := log.New(infoEndpoint, "BRENCIS INFO\t", log.Ldate|log.Ltime)
		brencisErrorLogger := log.New(os.Stdout, "BRENCIS ERROR\t", log.Ldate|log.Ltime|log.Lshortfile)

		go api.ListenAndServe(brencisKeyManager, brencisInfoLogger, brencisErrorLogger, config.BrencisAPiPort)
		go func() {
			for {
				time.Sleep(time.Second * 5)
				state := brencisKeyManager.GetFullState()
				brencisInfoLogger.Printf("state: %+v", state)
			}
		}()
	}

	time.Sleep(time.Millisecond * 100)

	log.Fatal(gatherer.Start())
}
