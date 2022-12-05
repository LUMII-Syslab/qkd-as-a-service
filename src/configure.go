package main

import (
	"log"

	"github.com/spf13/viper"
)

type Configuration struct {
	ClavisURL   string `mapstructure:"clavis_url"`
	MaxKeyCount int    `mapstructure:"max_key_cnt"`
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
	return res
}
