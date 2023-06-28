package main

import (
	"io"
	"log"
	"os"
	"qkdc-service/api"
	"qkdc-service/config"
	"qkdc-service/gatherers"
	"qkdc-service/manager"
	"time"

	"github.com/jedib0t/go-pretty/text"
)

func main() {
	app := NewApp()
	log.Fatal(app.Run())
}

type App struct {
	config        *config.Configuration
	gatherer      gatherers.KeyGatherer
	infoEndpoint  io.Writer
	debugEndpoint io.Writer
}

func NewApp() *App {
	app := &App{}
	app.config = config.LoadConfig("config.toml")
	app.config.Print()

	switch app.config.Gatherer {
	case "pseudorandom":
		app.gatherer = gatherers.NewPseudorandomKeyGatherer(32, 32, true)
	case "clavis":
		app.gatherer = gatherers.NewZeroMqKeyGatherer(app.config.ClavisURL)
	case "filesystem":
		app.gatherer = gatherers.NewFileSystemKeyGatherer(app.config.FSGathererDir)
	}

	if app.config.LogInfo {
		app.infoEndpoint = os.Stdout
	} else {
		app.infoEndpoint = io.Discard
	}

	if app.config.LogDebug {
		app.debugEndpoint = os.Stdout
	} else {
		app.debugEndpoint = io.Discard
	}

	return app
}

func (app *App) Run() error {
	if app.config.AijaEnabled {
		aijaDebugLogger := log.New(app.debugEndpoint, text.FgYellow.Sprint("AIJA DEBUG\t"), log.Ldate|log.Ltime|log.Lshortfile)
		aijaInfoLogger := log.New(app.infoEndpoint, text.FgBlue.Sprint("AIJA INFO\t"), log.Ldate|log.Ltime)
		aijaErrorLogger := log.New(os.Stderr, text.FgRed.Sprint("AIJA ERROR\t"), log.Ldate|log.Ltime|log.Lshortfile)

		aija := newAija(app.config.MaxKeyCount, app.config.DefaultServing, app.infoEndpoint, aijaDebugLogger)
		app.gatherer.PublishTo(aija)

		controller := api.NewController(aijaInfoLogger, aijaErrorLogger, aijaDebugLogger, aija)
		go controller.ListenAndServe(app.config.AijaAPIPort)

		go monitorKeyManager(aija, aijaInfoLogger)
	}

	if app.config.BrencisEnabled {
		brencisDebugLogger := log.New(app.debugEndpoint, text.FgYellow.Sprint("BRENCIS DEBUG\t"), log.Ldate|log.Ltime|log.Lshortfile)
		brencisInfoLogger := log.New(app.infoEndpoint, text.FgBlue.Sprint("BRENCIS INFO\t"), log.Ldate|log.Ltime)
		brencisErrorLogger := log.New(os.Stderr, text.FgRed.Sprint("BRENCIS ERROR\t"), log.Ldate|log.Ltime|log.Lshortfile)

		brencis := newBrencis(app.config.MaxKeyCount, app.config.DefaultServing, app.infoEndpoint, brencisDebugLogger)
		app.gatherer.PublishTo(brencis)

		controller := api.NewController(brencisInfoLogger, brencisErrorLogger, brencisDebugLogger, brencis)
		go controller.ListenAndServe(app.config.BrencisAPiPort)

		go monitorKeyManager(brencis, brencisInfoLogger)
	}

	time.Sleep(time.Millisecond * 100)

	return app.gatherer.Start()
}

func newAija(maxKeyCount uint64, defaultServing bool, infoEndpoint io.Writer, logger *log.Logger) *manager.KeyManager {
	aijaKeyManager := manager.NewKeyManager(maxKeyCount, true, logger, defaultServing)
	return aijaKeyManager
}

func newBrencis(maxKeyCount uint64, defaultServing bool, infoEndpoint io.Writer, logger *log.Logger) *manager.KeyManager {
	brencisKeyManager := manager.NewKeyManager(maxKeyCount, false, logger, defaultServing)
	return brencisKeyManager
}

func monitorKeyManager(keyManager *manager.KeyManager, logger *log.Logger) {
	lastAdded := uint64(0)
	TIMEOUT_SECONDS := 10.0
	for {
		time.Sleep(time.Second * time.Duration(TIMEOUT_SECONDS))
		state := keyManager.GetFullState()
		logger.Printf("reservable: %d; keys served: %d; keys added / s: %.2f", state.ReservableSize, state.KeysServed, float64(state.KeysAdded-lastAdded)/TIMEOUT_SECONDS)
		lastAdded = state.KeysAdded
	}
}
