package main

import (
	"io"
	"log"
	"os"
	"qkdc-service/api"
	"qkdc-service/gatherers"
	"qkdc-service/manager"
	"time"

	"github.com/jedib0t/go-pretty/text"
)

func main() {
	config := loadConfig()

	var gatherer gatherers.KeyGatherer
	switch config.Gatherer {
	case "pseudorandom":
		gatherer = gatherers.NewPseudorandomKeyGatherer(32, 32, true)
	case "clavis":
		gatherer = gatherers.NewClavisKeyGatherer(config.ClavisURL)
	case "filesystem":
		gatherer = gatherers.NewFileSystemKeyGatherer(config.FSGathererDir)
	}

	var infoEndpoint io.Writer = os.Stdout
	if !config.LogInfo {
		infoEndpoint = io.Discard
	}

	var debugEndpoint io.Writer = os.Stdout
	if !config.LogDebug {
		debugEndpoint = io.Discard
	}

	if config.AijaEnabled {
		aijaDebugLogger := log.New(debugEndpoint, text.FgYellow.Sprint("AIJA DEBUG\t"), log.Ldate|log.Ltime|log.Lshortfile)
		aijaKeyManager := manager.NewKeyManager(config.MaxKeyCount, true, aijaDebugLogger, config.DefaultServing)
		gatherer.PublishTo(aijaKeyManager)

		aijaInfoLogger := log.New(infoEndpoint, text.FgBlue.Sprint("AIJA INFO\t"), log.Ldate|log.Ltime)
		aijaErrorLogger := log.New(os.Stdout, text.FgRed.Sprint("AIJA ERROR\t"), log.Ldate|log.Ltime|log.Lshortfile)

		controller := api.NewController(aijaInfoLogger, aijaErrorLogger, aijaDebugLogger, aijaKeyManager)
		go controller.ListenAndServe(config.AijaAPIPort)
		go func() {
			for {
				time.Sleep(time.Second * 10)
				state := aijaKeyManager.GetFullState()
				aijaDebugLogger.Printf("state: %+v", state)
			}
		}()
	}

	if config.BrencisEnabled {
		brencisDebugLogger := log.New(debugEndpoint, text.FgYellow.Sprint("BRENCIS DEBUG\t"), log.Ldate|log.Ltime|log.Lshortfile)
		brencisKeyManager := manager.NewKeyManager(config.MaxKeyCount, false, brencisDebugLogger, config.DefaultServing)
		gatherer.PublishTo(brencisKeyManager)

		brencisInfoLogger := log.New(infoEndpoint, text.FgBlue.Sprint("BRENCIS INFO\t"), log.Ldate|log.Ltime)
		brencisErrorLogger := log.New(os.Stdout, text.FgRed.Sprint("BRENCIS ERROR\t"), log.Ldate|log.Ltime|log.Lshortfile)

		controller := api.NewController(brencisInfoLogger, brencisErrorLogger, brencisDebugLogger, brencisKeyManager)
		go controller.ListenAndServe(config.BrencisAPiPort)
		go func() {
			for {
				time.Sleep(time.Second * 10)
				state := brencisKeyManager.GetFullState()
				brencisDebugLogger.Printf("state: %+v", state)
			}
		}()
	}

	time.Sleep(time.Millisecond * 100)

	log.Fatal(gatherer.Start())
}
