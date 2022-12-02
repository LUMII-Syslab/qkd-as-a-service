package main

import (
	"log"

	"github.com/spf13/viper"
)

type Configuration struct {
	ClavisURL   string
	MaxKeyCount int
}

func LoadConfig() Configuration {
	res := Configuration{}
	viper.SetConfigName("config")
	viper.SetConfigType("toml")
	viper.AddConfigPath(".")
	err := viper.Unmarshal(&res)
	if err != nil {
		log.Panic(err)
	}
	return res
}
