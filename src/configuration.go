package main

import (
	"log"

	"github.com/spf13/viper"
)

type Configuration struct {
	ClavisURL   string `mapstructure:"clavis_url"`
	MaxKeyCount int    `mapstructure:"max_key_cnt"`
}

func LoadConfig() Configuration {
	res := Configuration{}
	viper.SetConfigFile("config.toml")
	confFile := viper.ConfigFileUsed()
	viper.ConfigFileUsed()
	viper.ReadInConfig()
	err := viper.Unmarshal(&res)
	if err != nil {
		log.Panic(err)
	}

	log.Printf("loaded \"%v\" as ClavisURL from %v\n", res.ClavisURL, confFile)
	log.Printf("loaded \"%v\" as MaxKeyCount from %v\n", res.MaxKeyCount, confFile)

	return res
}
