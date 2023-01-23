package main

import (
	"log"
	"qkdc-service/constants"

	"github.com/spf13/viper"
)

type Configuration struct {
	MaxKeyCount int `mapstructure:"max_key_cnt"`

	AijaAPIPort    int `mapstructure:"aija_port"`
	BrencisAPiPort int `mapstructure:"brencis_port"`
	AijaEnabled    bool
	BrencisEnabled bool

	Gatherer      string `mapstructure:"gatherer"`
	ClavisURL     string `mapstructure:"clavis_url"`
	FSGathererDir string `mapstructure:"fs_gatherer_dir"`

	LogRequests bool `mapstructure:"log_requests"`
}

func loadConfig() Configuration {
	res := Configuration{}
	viper.SetConfigFile("config.toml")
	confFile := viper.ConfigFileUsed()
	viper.ConfigFileUsed()
	err := viper.ReadInConfig()
	if err != nil {
		log.Panic(err)
	}
	err = viper.Unmarshal(&res)
	if err != nil {
		log.Panic(err)
	}

	if viper.IsSet("clavis_url") {
		log.Printf("loaded ClavisURL = \"%v\" from %v\n", res.ClavisURL, confFile)
	} else {
		log.Printf("loaded ClavisURL = \"%v\" from %v\n", res.ClavisURL, "defaults")
	}

	if viper.IsSet("max_key_cnt") {
		log.Printf("loaded MaxKeyCount = %v from %v\n", res.MaxKeyCount, confFile)
	} else {
		res.MaxKeyCount = constants.DefMxReq
		log.Printf("loaded MaxKeyCount = %v from %v\n", res.MaxKeyCount, "defaults")
	}

	if viper.IsSet("aija_port") {
		res.AijaEnabled = true
		log.Printf("loaded Aija = %v from %v\n", res.AijaAPIPort, confFile)
	} else {
		res.AijaEnabled = false
		log.Printf("loaded Aija = %v from %v\n", res.AijaAPIPort, "defaults")
	}

	if viper.IsSet("brencis_port") {
		res.BrencisEnabled = true
		log.Printf("loaded APIPort = %v from %v\n", res.BrencisAPiPort, confFile)
	} else {
		res.BrencisEnabled = false
		log.Printf("loaded APIPort = %v from %v\n", res.BrencisAPiPort, "defaults")
	}

	if viper.IsSet("log_requests") {
		log.Printf("loaded LogRequests = %v from %v\n", res.LogRequests, confFile)
	} else {
		res.LogRequests = false
		log.Printf("loaded LogRequests = %v from %v\n", res.LogRequests, "defaults")
	}

	if res.AijaAPIPort == res.BrencisAPiPort {
		log.Panic("Aija and Brencis ports must be different")
	}

	if viper.IsSet("gatherer") {
		log.Printf("loaded Gatherer = %v from %v\n", res.Gatherer, confFile)
	} else {
		res.Gatherer = "pseudorandom"
		log.Printf("loaded Gatherer = %v from %v\n", res.Gatherer, "defaults")
	}

	if viper.IsSet("fs_gatherer_dir") {
		log.Printf("loaded FSGathererDir = %v from %v\n", res.FSGathererDir, confFile)
	} else {
		res.FSGathererDir = "/tmp/qkd-mock"
		log.Printf("loaded FSGathererDir = %v from %v\n", res.FSGathererDir, "defaults")
	}

	return res
}
