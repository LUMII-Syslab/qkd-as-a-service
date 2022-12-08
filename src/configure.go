package main

import (
	"log"

	"github.com/spf13/viper"
)

type Configuration struct {
	ClavisURL   string `mapstructure:"clavis_url"`
	MaxKeyCount int    `mapstructure:"max_key_cnt"`
	Aija        bool   `mapstructure:"aija"`
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
		log.Printf("loaded \"%v\" as ClavisURL from %v\n", res.ClavisURL, confFile)
	} else {
		log.Printf("\"clavis_url\" not found in config")
		log.Printf("set \"%v\" as ClavisURL", res.ClavisURL)
	}

	if viper.IsSet("max_key_cnt") {
		log.Printf("loaded %v as MaxKeyCount from %v\n", res.MaxKeyCount, confFile)
	} else {
		log.Printf("\"max_key_cnt\" not found in config")
		res.MaxKeyCount = 1000
		log.Printf("set %v as MaxKeyCount", res.MaxKeyCount)
	}

	if viper.IsSet("aija") {
		log.Printf("loaded %v as Aija from %v\n", res.Aija, confFile)
	} else {
		log.Printf("\"aija\" not found in config")
		res.Aija = true
		log.Printf("set %v as aija", res.Aija)
	}
	return res
}
