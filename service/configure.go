package main

import (
	"log"

	"github.com/spf13/viper"
)

type Configuration struct {
	ClavisURL      string `mapstructure:"clavis_url"`
	MaxKeyCount    int    `mapstructure:"max_key_cnt"`
	AijaAPIPort    int    `mapstructure:"aija_port"`
	BrencisAPiPort int    `mapstructure:"brencis_port"`
	LogRequests    bool   `mapstructure:"log_requests"`
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
		res.MaxKeyCount = 1000
		log.Printf("loaded MaxKeyCount = %v from %v\n", res.MaxKeyCount, "defaults")
	}

	if viper.IsSet("aija_port") {
		log.Printf("loaded Aija = %v from %v\n", res.AijaAPIPort, confFile)
	} else {
		res.AijaAPIPort = -1
		log.Printf("loaded Aija = %v from %v\n", res.AijaAPIPort, "defaults")
	}

	if viper.IsSet("brencis_port") {
		log.Printf("loaded APIPort = %v from %v\n", res.BrencisAPiPort, confFile)
	} else {
		res.BrencisAPiPort = -1
		log.Printf("loaded APIPort = %v from %v\n", res.BrencisAPiPort, "defaults")
	}

	if viper.IsSet("log_requests") {
		log.Printf("loaded LogRequests = %v from %v\n", res.LogRequests, confFile)
	} else {
		res.LogRequests = false
		log.Printf("loaded LogRequests = %v from %v\n", res.LogRequests, "defaults")
	}

	return res
}
